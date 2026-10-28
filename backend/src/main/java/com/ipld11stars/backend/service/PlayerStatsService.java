package com.ipld11stars.backend.service;

import com.ipld11stars.backend.model.Delivery;
import com.ipld11stars.backend.model.PlayerMatchStats;
import com.ipld11stars.backend.model.PlayerRole;
import com.ipld11stars.backend.model.SquadMember;
import com.ipld11stars.backend.repository.jdbc.DeliveryRepository;
import com.ipld11stars.backend.repository.jdbc.MatchSquadRepository;
import com.ipld11stars.backend.repository.jdbc.PlayerMatchStatsRepository;
import com.ipld11stars.backend.scoring.FantasyPointsCalculator;
import com.ipld11stars.backend.scoring.MatchStatsAggregator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recomputes every player's figures and fantasy points for a match from the ball-by-ball record.
 * Always a full rebuild, so it is safe to call after any change to the match's deliveries.
 */
@Service
public class PlayerStatsService {

    private final DeliveryRepository deliveryRepository;
    private final MatchSquadRepository squadRepository;
    private final PlayerMatchStatsRepository statsRepository;
    private final MatchStatsAggregator aggregator;
    private final FantasyPointsCalculator calculator;

    public PlayerStatsService(DeliveryRepository deliveryRepository, MatchSquadRepository squadRepository,
                              PlayerMatchStatsRepository statsRepository, MatchStatsAggregator aggregator,
                              FantasyPointsCalculator calculator) {
        this.deliveryRepository = deliveryRepository;
        this.squadRepository = squadRepository;
        this.statsRepository = statsRepository;
        this.aggregator = aggregator;
        this.calculator = calculator;
    }

    @Transactional
    public List<PlayerMatchStats> recompute(String matchId) {
        List<Delivery> deliveries = deliveryRepository.findByMatchId(matchId);
        Map<String, PlayerMatchStats> stats = aggregator.aggregate(matchId, deliveries);

        // Everyone in the squad gets a row so the starting XI bonus applies even without involvement.
        Set<String> scored = new HashSet<>();
        for (SquadMember member : squadRepository.findByMatchId(matchId)) {
            PlayerMatchStats s = stats.computeIfAbsent(member.playerId(), id -> new PlayerMatchStats(matchId, id));
            s.setPoints(calculator.calculate(s, member.role(), member.startingXi()));
            scored.add(member.playerId());
        }
        for (PlayerMatchStats s : stats.values()) {
            if (!scored.contains(s.getPlayerId())) {
                // Deliveries are validated against the squad, so this only covers legacy data.
                s.setPoints(calculator.calculate(s, PlayerRole.BAT, false));
            }
        }
        statsRepository.replaceForMatch(matchId, stats.values());
        return statsRepository.findByMatchId(matchId);
    }

    public List<PlayerMatchStats> getStats(String matchId) {
        return statsRepository.findByMatchId(matchId);
    }
}
