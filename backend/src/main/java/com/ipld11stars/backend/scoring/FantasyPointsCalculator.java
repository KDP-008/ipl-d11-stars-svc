package com.ipld11stars.backend.scoring;

import com.ipld11stars.backend.model.PlayerMatchStats;
import com.ipld11stars.backend.model.PlayerRole;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.ipld11stars.backend.scoring.ScoringRules.*;

/**
 * Turns a player's match figures into fantasy points. Pure function of its inputs.
 */
@Component
public class FantasyPointsCalculator {

    public BigDecimal calculate(PlayerMatchStats s, PlayerRole role, boolean startingXi) {
        int points = 0;

        // Batting
        points += s.getRunsScored() * RUN;
        points += s.getFours() * BOUNDARY_BONUS;
        points += s.getSixes() * SIX_BONUS;
        if (s.getRunsScored() >= 100) {
            points += CENTURY_BONUS;
        } else if (s.getRunsScored() >= 50) {
            points += HALF_CENTURY_BONUS;
        } else if (s.getRunsScored() >= 30) {
            points += THIRTY_RUN_BONUS;
        }
        if (s.isDismissed() && s.getRunsScored() == 0 && role != PlayerRole.BOWL) {
            points += DUCK;
        }
        points += strikeRatePoints(s, role);

        // Bowling
        points += s.getWickets() * WICKET;
        points += s.getLbwBowled() * LBW_BOWLED_BONUS;
        if (s.getWickets() >= 3) {
            points += THREE_WICKET_BONUS;
        }
        points += s.getMaidens() * MAIDEN;
        points += s.getDotBalls() * DOT_BALL;
        points += economyPoints(s);

        // Fielding
        points += s.getCatches() * CATCH;
        points += s.getStumpings() * STUMPING;
        points += s.getRunOuts() * RUN_OUT;

        if (startingXi) {
            points += STARTING_XI;
        }
        return BigDecimal.valueOf(points).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Runs per 100 balls, rounded to 2 dp; null when no balls faced. */
    public static BigDecimal strikeRate(int runs, int balls) {
        if (balls == 0) {
            return null;
        }
        return BigDecimal.valueOf(runs * 100L).divide(BigDecimal.valueOf(balls), 2, RoundingMode.HALF_UP);
    }

    /** Runs per six legal balls, rounded to 2 dp; null when no legal balls bowled. */
    public static BigDecimal economy(int runsConceded, int legalBalls) {
        if (legalBalls == 0) {
            return null;
        }
        return BigDecimal.valueOf(runsConceded * 6L).divide(BigDecimal.valueOf(legalBalls), 2, RoundingMode.HALF_UP);
    }

    int strikeRatePoints(PlayerMatchStats s, PlayerRole role) {
        if (role == PlayerRole.BOWL || s.getBallsFaced() < STRIKE_RATE_MIN_BALLS) {
            return 0;
        }
        BigDecimal sr = strikeRate(s.getRunsScored(), s.getBallsFaced());
        if (sr.compareTo(bd("170")) > 0) return 6;
        if (sr.compareTo(bd("150")) > 0) return 4;
        if (sr.compareTo(bd("130")) >= 0) return 2;
        if (sr.compareTo(bd("70")) > 0) return 0;
        if (sr.compareTo(bd("60")) >= 0) return -2;
        if (sr.compareTo(bd("50")) >= 0) return -4;
        return -6;
    }

    int economyPoints(PlayerMatchStats s) {
        if (s.getBallsBowled() < ECONOMY_MIN_BALLS) {
            return 0;
        }
        BigDecimal econ = economy(s.getRunsConceded(), s.getBallsBowled());
        if (econ.compareTo(bd("5")) < 0) return 6;
        if (econ.compareTo(bd("6")) < 0) return 4;
        if (econ.compareTo(bd("7")) <= 0) return 2;
        if (econ.compareTo(bd("10")) < 0) return 0;
        if (econ.compareTo(bd("11")) <= 0) return -2;
        if (econ.compareTo(bd("12")) <= 0) return -4;
        return -6;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
