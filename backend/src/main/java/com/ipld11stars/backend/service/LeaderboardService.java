package com.ipld11stars.backend.service;

import com.ipld11stars.backend.model.Contest;
import com.ipld11stars.backend.model.ContestEntry;
import com.ipld11stars.backend.model.EntrySelection;
import com.ipld11stars.backend.model.PlayerMatchStats;
import com.ipld11stars.backend.repository.jdbc.ContestEntryRepository;
import com.ipld11stars.backend.repository.jdbc.ContestRepository;
import com.ipld11stars.backend.repository.jdbc.PlayerMatchStatsRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Live contest leaderboards. An entry scores the sum of its players' fantasy points, with the
 * captain counted twice and the vice-captain one and a half times. Ranks use standard competition
 * ranking ("1224"): tied totals share a rank and the next rank skips accordingly.
 */
@Service
@Order(2)
public class LeaderboardService implements MatchProgressListener {

    private static final BigDecimal CAPTAIN = new BigDecimal("2");
    private static final BigDecimal VICE_CAPTAIN = new BigDecimal("1.5");
    private static final List<String> LIVE_STATUSES = List.of("LOCKED", "LIVE");

    private final ContestRepository contestRepository;
    private final ContestEntryRepository entryRepository;
    private final PlayerMatchStatsRepository statsRepository;

    public LeaderboardService(ContestRepository contestRepository, ContestEntryRepository entryRepository,
                              PlayerMatchStatsRepository statsRepository) {
        this.contestRepository = contestRepository;
        this.entryRepository = entryRepository;
        this.statsRepository = statsRepository;
    }

    @Override
    public void onDeliveryRecorded(String matchId) {
        refreshForMatch(matchId);
    }

    @Transactional
    public void refreshForMatch(String matchId) {
        Map<String, BigDecimal> points = new HashMap<>();
        for (PlayerMatchStats s : statsRepository.findByMatchId(matchId)) {
            points.put(s.getPlayerId(), s.getPoints());
        }
        for (Contest contest : contestRepository.findByMatchIdAndStatusIn(matchId, LIVE_STATUSES)) {
            contestRepository.transitionStatus(contest.getId(), List.of("LOCKED"), "LIVE");
            refreshContest(contest.getId(), points);
        }
    }

    /** Recomputes totals and ranks for one contest and returns the entries in leaderboard order. */
    @Transactional
    public List<ContestEntry> refreshContest(String contestId, Map<String, BigDecimal> playerPoints) {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (EntrySelection s : entryRepository.findSelectionsByContestId(contestId)) {
            BigDecimal base = playerPoints.getOrDefault(s.playerId(), BigDecimal.ZERO);
            BigDecimal multiplier = s.captain() ? CAPTAIN : s.viceCaptain() ? VICE_CAPTAIN : BigDecimal.ONE;
            totals.merge(s.entryId(), base.multiply(multiplier), BigDecimal::add);
        }

        List<ContestEntry> entries = new ArrayList<>(entryRepository.findByContestId(contestId));
        for (ContestEntry e : entries) {
            e.setTotalPoints(totals.getOrDefault(e.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }
        entries.sort(Comparator.comparing(ContestEntry::getTotalPoints).reversed()
                .thenComparing(ContestEntry::getCreatedAt)
                .thenComparing(ContestEntry::getId));

        BigDecimal previous = null;
        int rank = 0;
        for (int i = 0; i < entries.size(); i++) {
            ContestEntry e = entries.get(i);
            if (previous == null || e.getTotalPoints().compareTo(previous) != 0) {
                rank = i + 1;
                previous = e.getTotalPoints();
            }
            e.setRank(rank);
            entryRepository.updatePointsAndRank(e.getId(), e.getTotalPoints(), rank);
        }
        return entries;
    }

    public List<ContestEntry> getLeaderboard(String contestId) {
        return entryRepository.findByContestId(contestId);
    }

    /** Current points for every player in the match, keyed by player id. */
    public Map<String, BigDecimal> playerPoints(String matchId) {
        Map<String, BigDecimal> points = new HashMap<>();
        statsRepository.findByMatchId(matchId).forEach(s -> points.put(s.getPlayerId(), s.getPoints()));
        return points;
    }
}
