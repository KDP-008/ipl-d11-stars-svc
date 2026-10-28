package com.ipld11stars.backend.service;

import com.ipld11stars.backend.dto.JoinContestRequest;
import com.ipld11stars.backend.exception.BusinessRuleException;
import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestEntry;
import com.ipld11stars.backend.model.EntrySelection;
import com.ipld11stars.backend.model.Match;
import com.ipld11stars.backend.model.PlayerRole;
import com.ipld11stars.backend.model.SquadMember;
import com.ipld11stars.backend.model.TransactionType;
import com.ipld11stars.backend.notification.EmailTemplate;
import com.ipld11stars.backend.notification.NotificationService;
import com.ipld11stars.backend.repository.jdbc.ContestEntryRepository;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import com.ipld11stars.backend.repository.jdbc.MatchRepository;
import com.ipld11stars.backend.repository.jdbc.MatchSquadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Joining contests: team selection rules, entry fee collection and slot accounting.
 *
 * Team rules: exactly 11 players from the match squads, at most 100 credits, at most 7 from one
 * side, 1-4 wicket-keepers, 3-6 batters, 1-4 all-rounders, 3-6 bowlers, and a distinct captain
 * and vice-captain from the picked players.
 */
@Service
public class ContestEntryService {

    public static final int TEAM_SIZE = 11;
    public static final BigDecimal CREDIT_LIMIT = new BigDecimal("100.0");
    public static final int MAX_FROM_ONE_SIDE = 7;

    private final ContestRepository contestRepository;
    private final ContestEntryRepository entryRepository;
    private final MatchRepository matchRepository;
    private final MatchSquadRepository squadRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final Clock clock;

    public ContestEntryService(ContestRepository contestRepository, ContestEntryRepository entryRepository,
                               MatchRepository matchRepository, MatchSquadRepository squadRepository,
                               WalletService walletService, NotificationService notificationService, Clock clock) {
        this.contestRepository = contestRepository;
        this.entryRepository = entryRepository;
        this.matchRepository = matchRepository;
        this.squadRepository = squadRepository;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    public ContestEntry join(String userId, String contestId, JoinContestRequest request) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> BusinessRuleException.notFound("Contest not found: " + contestId));
        Match match = matchRepository.findById(contest.getMatchId())
                .orElseThrow(() -> BusinessRuleException.notFound("Match not found for contest " + contestId));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!"OPEN".equals(contest.getStatus()) || !"UPCOMING".equals(match.getMatchStatus())
                || (match.getStartTimeUtc() != null && !now.isBefore(match.getStartTimeUtc()))) {
            throw BusinessRuleException.conflict("This contest is no longer accepting entries");
        }
        if (entryRepository.findByContestAndUser(contestId, userId).isPresent()) {
            throw BusinessRuleException.conflict("You have already joined this contest");
        }

        List<EntrySelection> selections = validateTeam(match, request);

        if (!contestRepository.tryTakeEntrySlot(contestId)) {
            throw BusinessRuleException.conflict("This contest is full");
        }
        String entryId = UUID.randomUUID().toString();
        BigDecimal fee = contest.getEntryFee() != null ? contest.getEntryFee().setScale(2) : BigDecimal.ZERO.setScale(2);
        if (fee.signum() > 0) {
            walletService.debit(userId, fee, TransactionType.ENTRY_FEE, "CONTEST_ENTRY", entryId,
                    "entry-fee:" + contestId + ":" + userId, "Entry fee for " + contest.getName());
        }

        ContestEntry entry = new ContestEntry();
        entry.setId(entryId);
        entry.setContestId(contestId);
        entry.setUserId(userId);
        entry.setEntryName(request.entryName() != null && !request.entryName().isBlank() ? request.entryName().trim() : "Team 1");
        entry.setEntryFeePaid(fee);
        entry.setCreatedAt(now);
        entryRepository.insert(entry);
        entryRepository.insertSelections(entryId, selections.stream()
                .map(s -> new EntrySelection(entryId, s.playerId(), s.captain(), s.viceCaptain())).toList());

        notificationService.notifyUser(userId, EmailTemplate.ENTRY_CONFIRMED,
                Map.of("contestName", contest.getName(), "entryName", entry.getEntryName(), "entryFee", fee));
        return entryRepository.findByContestAndUser(contestId, userId).orElseThrow();
    }

    public ContestEntry getEntry(String userId, String contestId) {
        return entryRepository.findByContestAndUser(contestId, userId)
                .orElseThrow(() -> BusinessRuleException.notFound("You have not joined this contest"));
    }

    public List<EntrySelection> getSelections(String entryId) {
        return entryRepository.findSelections(entryId);
    }

    private List<EntrySelection> validateTeam(Match match, JoinContestRequest request) {
        if (request == null || request.playerIds() == null) {
            throw new BusinessRuleException("Pick " + TEAM_SIZE + " players");
        }
        List<String> picked = request.playerIds();
        Set<String> unique = new HashSet<>(picked);
        if (picked.size() != TEAM_SIZE || unique.size() != TEAM_SIZE) {
            throw new BusinessRuleException("A team needs exactly " + TEAM_SIZE + " different players");
        }
        Map<String, SquadMember> squad = squadRepository.findByMatchId(match.getId()).stream()
                .collect(Collectors.toMap(SquadMember::playerId, Function.identity()));
        List<SquadMember> members = new ArrayList<>();
        for (String id : picked) {
            SquadMember m = squad.get(id);
            if (m == null) {
                throw new BusinessRuleException("Player " + id + " is not in either squad for this match");
            }
            members.add(m);
        }

        BigDecimal credits = members.stream().map(SquadMember::credits).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (credits.compareTo(CREDIT_LIMIT) > 0) {
            throw new BusinessRuleException("Team uses " + credits + " credits; the limit is " + CREDIT_LIMIT);
        }

        Map<String, Integer> perSide = new HashMap<>();
        members.forEach(m -> perSide.merge(m.teamId(), 1, Integer::sum));
        if (perSide.values().stream().anyMatch(n -> n > MAX_FROM_ONE_SIDE)) {
            throw new BusinessRuleException("At most " + MAX_FROM_ONE_SIDE + " players can come from one side");
        }

        Map<PlayerRole, Integer> perRole = new EnumMap<>(PlayerRole.class);
        members.forEach(m -> perRole.merge(m.role(), 1, Integer::sum));
        for (PlayerRole role : PlayerRole.values()) {
            int n = perRole.getOrDefault(role, 0);
            if (n < role.getMinPerTeam() || n > role.getMaxPerTeam()) {
                throw new BusinessRuleException("Pick between " + role.getMinPerTeam() + " and " + role.getMaxPerTeam()
                        + " " + role + " players (you picked " + n + ")");
            }
        }

        String c = request.captainId();
        String vc = request.viceCaptainId();
        if (c == null || vc == null || !unique.contains(c) || !unique.contains(vc)) {
            throw new BusinessRuleException("Captain and vice-captain must be among the picked players");
        }
        if (c.equals(vc)) {
            throw new BusinessRuleException("Captain and vice-captain must be different players");
        }
        return picked.stream().map(id -> new EntrySelection(null, id, id.equals(c), id.equals(vc))).toList();
    }
}
