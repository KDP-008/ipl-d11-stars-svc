package com.ipld11stars.backend.service;

import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestEntry;
import com.ipld11stars.backend.model.ContestPrize;
import com.ipld11stars.backend.model.Match;
import com.ipld11stars.backend.model.TransactionType;
import com.ipld11stars.backend.notification.EmailTemplate;
import com.ipld11stars.backend.notification.NotificationService;
import com.ipld11stars.backend.repository.jdbc.ContestEntryRepository;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Contest lifecycle: OPEN -> LOCKED (match start) -> LIVE (scoring) -> COMPLETED (settled),
 * or CANCELLED with full refunds when too few users joined or the match is abandoned.
 *
 * Settlement pays the prize table by finishing position. Positions follow the leaderboard order
 * (points, then earliest entry, then entry id), so when totals are tied the earlier entry takes the
 * higher position and its prize; the displayed rank is still shared.
 */
@Service
@Order(1)
public class ContestLifecycleService implements MatchProgressListener {

    private static final Logger log = LoggerFactory.getLogger(ContestLifecycleService.class);

    private final ContestRepository contestRepository;
    private final ContestEntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final ContestService contestService;
    private final LeaderboardService leaderboardService;
    private final PlayerStatsService playerStatsService;
    private final DeliveryService deliveryService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final Clock clock;

    public ContestLifecycleService(ContestRepository contestRepository, ContestEntryRepository entryRepository,
                                   MatchRepository matchRepository, ContestService contestService,
                                   LeaderboardService leaderboardService, PlayerStatsService playerStatsService,
                                   @org.springframework.context.annotation.Lazy DeliveryService deliveryService,
                                   WalletService walletService, NotificationService notificationService, Clock clock) {
        this.contestRepository = contestRepository;
        this.entryRepository = entryRepository;
        this.matchRepository = matchRepository;
        this.contestService = contestService;
        this.leaderboardService = leaderboardService;
        this.playerStatsService = playerStatsService;
        this.deliveryService = deliveryService;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Override
    public void onMatchStarting(String matchId) {
        lockContestsForMatch(matchId);
    }

    /** Locks (or cancels) open contests for every match whose start time has passed. */
    @Transactional
    public int lockDueContests() {
        LocalDateTime now = LocalDateTime.now(clock);
        int handled = 0;
        for (Match match : matchRepository.findAll()) {
            if (match.getStartTimeUtc() != null && !now.isBefore(match.getStartTimeUtc())) {
                handled += lockContestsForMatch(match.getId());
            }
        }
        return handled;
    }

    @Transactional
    public int lockContestsForMatch(String matchId) {
        int handled = 0;
        for (Contest contest : contestRepository.findByMatchIdAndStatusIn(matchId, List.of("OPEN"))) {
            int min = contest.getMinEntries() != null ? contest.getMinEntries() : 2;
            if (contest.getCurrentEntries() < min) {
                cancel(contest, List.of("OPEN"), "only " + contest.getCurrentEntries() + " of the required " + min + " entries joined");
            } else if (contestRepository.transitionStatus(contest.getId(), List.of("OPEN"), "LOCKED")) {
                log.info("Locked contest {} with {} entries", contest.getId(), contest.getCurrentEntries());
            }
            handled++;
        }
        return handled;
    }

    /** Ends the match once the result is in and settles every contest on it. */
    @Transactional
    public void completeMatch(String matchId) {
        Match match = requireMatch(matchId);
        if (!"LIVE".equals(match.getMatchStatus())) {
            throw BusinessRuleException.conflict("Only a live match can be completed (match is " + match.getMatchStatus() + ")");
        }
        if (!deliveryService.isMatchResultReached(matchId)) {
            throw BusinessRuleException.conflict("The second innings is not finished yet");
        }
        match.setMatchStatus("COMPLETED");
        matchRepository.save(match);

        lockContestsForMatch(matchId);
        playerStatsService.recompute(matchId);
        Map<String, BigDecimal> points = leaderboardService.playerPoints(matchId);
        for (Contest contest : contestRepository.findByMatchIdAndStatusIn(matchId, List.of("LOCKED", "LIVE"))) {
            settle(contest, points);
        }
    }

    /** Abandons the match and refunds every contest that had not finished. */
    @Transactional
    public void abandonMatch(String matchId) {
        Match match = requireMatch(matchId);
        if (!"UPCOMING".equals(match.getMatchStatus()) && !"LIVE".equals(match.getMatchStatus())) {
            throw BusinessRuleException.conflict("Match is already " + match.getMatchStatus());
        }
        match.setMatchStatus("ABANDONED");
        matchRepository.save(match);
        for (Contest contest : contestRepository.findByMatchIdAndStatusIn(matchId, List.of("OPEN", "LOCKED", "LIVE"))) {
            cancel(contest, List.of("OPEN", "LOCKED", "LIVE"), "the match was abandoned");
        }
    }

    private void settle(Contest contest, Map<String, BigDecimal> points) {
        if (!contestRepository.transitionStatus(contest.getId(), List.of("LOCKED", "LIVE"), "COMPLETED")) {
            return;
        }
        List<ContestEntry> ordered = leaderboardService.refreshContest(contest.getId(), points);
        List<ContestPrize> prizes = contestService.getPrizeTable(contest);
        for (int i = 0; i < ordered.size(); i++) {
            ContestEntry entry = ordered.get(i);
            BigDecimal prize = PrizeTable.prizeForPosition(prizes, i + 1);
            if (prize.signum() > 0) {
                walletService.credit(entry.getUserId(), prize, TransactionType.PRIZE, "CONTEST_ENTRY", entry.getId(),
                        "prize:" + entry.getId(), "Winnings from " + contest.getName());
            }
            entryRepository.markSettled(entry.getId(), prize);
            notificationService.notifyUser(entry.getUserId(), EmailTemplate.CONTEST_RESULT, Map.of(
                    "contestName", contest.getName(), "entryName", entry.getEntryName(), "rank", entry.getRank(),
                    "points", entry.getTotalPoints(), "prize", prize));
        }
        log.info("Settled contest {} across {} entries", contest.getId(), ordered.size());
    }

    private void cancel(Contest contest, List<String> fromStatuses, String reason) {
        if (!contestRepository.transitionStatus(contest.getId(), fromStatuses, "CANCELLED")) {
            return;
        }
        for (ContestEntry entry : entryRepository.findByContestId(contest.getId())) {
            if (!"ACTIVE".equals(entry.getStatus())) {
                continue;
            }
            if (entry.getEntryFeePaid() != null && entry.getEntryFeePaid().signum() > 0) {
                walletService.credit(entry.getUserId(), entry.getEntryFeePaid(), TransactionType.ENTRY_REFUND,
                        "CONTEST_ENTRY", entry.getId(), "refund:" + entry.getId(), "Refund for " + contest.getName());
            }
            entryRepository.markRefunded(entry.getId());
            notificationService.notifyUser(entry.getUserId(), EmailTemplate.CONTEST_CANCELLED, Map.of(
                    "contestName", contest.getName(), "reason", reason, "entryFee", entry.getEntryFeePaid()));
        }
        log.info("Cancelled contest {}: {}", contest.getId(), reason);
    }

    private Match requireMatch(String matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> BusinessRuleException.notFound("Match not found: " + matchId));
    }
}
