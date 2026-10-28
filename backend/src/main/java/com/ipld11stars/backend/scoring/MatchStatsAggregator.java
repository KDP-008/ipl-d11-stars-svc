package com.ipld11stars.backend.scoring;

import com.ipld11stars.backend.model.Delivery;
import com.ipld11stars.backend.model.DismissalType;
import com.ipld11stars.backend.model.ExtrasType;
import com.ipld11stars.backend.model.PlayerMatchStats;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ipld11stars.backend.scoring.ScoringRules.BALLS_PER_OVER;

/**
 * Builds per-player match figures from the ball-by-ball record.
 *
 * Conventions:
 * - balls faced counts every delivery except wides (no-balls, byes and leg-byes all count);
 * - runs conceded = runs off the bat + wide and no-ball extras (byes and leg-byes are not charged);
 * - a dot ball is a legal delivery on which the bowler concedes nothing;
 * - a maiden is a completed six-legal-ball over in which the bowler conceded nothing.
 */
@Component
public class MatchStatsAggregator {

    public Map<String, PlayerMatchStats> aggregate(String matchId, List<Delivery> deliveries) {
        Map<String, PlayerMatchStats> stats = new LinkedHashMap<>();

        // innings -> over -> deliveries in that over
        Map<Integer, Map<Integer, List<Delivery>>> overs = new LinkedHashMap<>();

        for (Delivery d : deliveries) {
            PlayerMatchStats striker = stats.computeIfAbsent(d.strikerId(), id -> new PlayerMatchStats(matchId, id));
            stats.computeIfAbsent(d.nonStrikerId(), id -> new PlayerMatchStats(matchId, id));
            PlayerMatchStats bowler = stats.computeIfAbsent(d.bowlerId(), id -> new PlayerMatchStats(matchId, id));

            striker.setRunsScored(striker.getRunsScored() + d.runsBatter());
            if (d.extrasType() != ExtrasType.WIDE) {
                striker.setBallsFaced(striker.getBallsFaced() + 1);
            }
            if (d.runsBatter() == 4) {
                striker.setFours(striker.getFours() + 1);
            } else if (d.runsBatter() == 6) {
                striker.setSixes(striker.getSixes() + 1);
            }

            bowler.setRunsConceded(bowler.getRunsConceded() + d.runsConcededByBowler());
            if (d.legal()) {
                bowler.setBallsBowled(bowler.getBallsBowled() + 1);
                if (d.runsConcededByBowler() == 0) {
                    bowler.setDotBalls(bowler.getDotBalls() + 1);
                }
            }

            if (d.wicket()) {
                DismissalType type = d.dismissalType();
                PlayerMatchStats out = stats.computeIfAbsent(d.dismissedPlayerId(), id -> new PlayerMatchStats(matchId, id));
                out.setDismissed(true);
                if (type.isCreditedToBowler()) {
                    bowler.setWickets(bowler.getWickets() + 1);
                    if (type.isBowledOrLbw()) {
                        bowler.setLbwBowled(bowler.getLbwBowled() + 1);
                    }
                }
                if (d.fielderId() != null) {
                    PlayerMatchStats fielder = stats.computeIfAbsent(d.fielderId(), id -> new PlayerMatchStats(matchId, id));
                    switch (type) {
                        case CAUGHT -> fielder.setCatches(fielder.getCatches() + 1);
                        case STUMPED -> fielder.setStumpings(fielder.getStumpings() + 1);
                        case RUN_OUT -> fielder.setRunOuts(fielder.getRunOuts() + 1);
                        default -> { }
                    }
                }
            }

            overs.computeIfAbsent(d.innings(), i -> new LinkedHashMap<>())
                    .computeIfAbsent(d.overNumber(), o -> new ArrayList<>())
                    .add(d);
        }

        countMaidens(overs, stats);

        for (PlayerMatchStats s : stats.values()) {
            s.setStrikeRate(FantasyPointsCalculator.strikeRate(s.getRunsScored(), s.getBallsFaced()));
            s.setEconomyRate(FantasyPointsCalculator.economy(s.getRunsConceded(), s.getBallsBowled()));
        }
        return stats;
    }

    private void countMaidens(Map<Integer, Map<Integer, List<Delivery>>> overs, Map<String, PlayerMatchStats> stats) {
        for (Map<Integer, List<Delivery>> inningsOvers : overs.values()) {
            for (List<Delivery> over : inningsOvers.values()) {
                long legal = over.stream().filter(Delivery::legal).count();
                int conceded = over.stream().mapToInt(Delivery::runsConcededByBowler).sum();
                if (legal == BALLS_PER_OVER && conceded == 0) {
                    PlayerMatchStats bowler = stats.get(over.get(0).bowlerId());
                    bowler.setMaidens(bowler.getMaidens() + 1);
                }
            }
        }
    }
}
