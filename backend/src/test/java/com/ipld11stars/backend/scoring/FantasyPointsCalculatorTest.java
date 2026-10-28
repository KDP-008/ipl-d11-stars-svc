package com.ipld11stars.backend.scoring;

import com.ipld11stars.backend.model.PlayerMatchStats;
import com.ipld11stars.backend.model.PlayerRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FantasyPointsCalculatorTest {

    private final FantasyPointsCalculator calculator = new FantasyPointsCalculator();

    private static PlayerMatchStats batting(int runs, int balls, int fours, int sixes, boolean out) {
        PlayerMatchStats s = new PlayerMatchStats("m", "p");
        s.setRunsScored(runs);
        s.setBallsFaced(balls);
        s.setFours(fours);
        s.setSixes(sixes);
        s.setDismissed(out);
        return s;
    }

    private static PlayerMatchStats bowling(int balls, int conceded, int wickets, int lbwBowled, int maidens, int dots) {
        PlayerMatchStats s = new PlayerMatchStats("m", "p");
        s.setBallsBowled(balls);
        s.setRunsConceded(conceded);
        s.setWickets(wickets);
        s.setLbwBowled(lbwBowled);
        s.setMaidens(maidens);
        s.setDotBalls(dots);
        return s;
    }

    private int points(PlayerMatchStats s, PlayerRole role, boolean xi) {
        return calculator.calculate(s, role, xi).intValueExact();
    }

    @Test
    void halfCenturyGetsOnlyTheHighestMilestoneAndStrikeRateBonus() {
        // 52 off 30 (SR 173.33): 52 + 4*4 + 2*6 + 8 (fifty) + 6 (SR > 170)
        assertThat(points(batting(52, 30, 4, 2, false), PlayerRole.BAT, false)).isEqualTo(52 + 16 + 12 + 8 + 6);
    }

    @Test
    void centuryReplacesFiftyBonus() {
        // 100 off 60 (SR 166.67): 100 + 16 (century) + 4 (SR 150-170)
        assertThat(points(batting(100, 60, 0, 0, true), PlayerRole.BAT, false)).isEqualTo(120);
    }

    @Test
    void duckAppliesToBattersButNotBowlers() {
        assertThat(points(batting(0, 1, 0, 0, true), PlayerRole.WK, false)).isEqualTo(-2);
        assertThat(points(batting(0, 1, 0, 0, true), PlayerRole.BOWL, false)).isZero();
        assertThat(points(batting(0, 3, 0, 0, false), PlayerRole.BAT, false)).as("not out on 0 is not a duck").isZero();
    }

    @Test
    void strikeRateBracketsAreInclusiveWhereTheTableSaysSo() {
        // 13 off 10 = 130.00 -> +2 ; 7 off 10 = 70.00 -> -2 ; 5 off 10 = 50.00 -> -4 ; 4 off 10 -> -6
        assertThat(points(batting(13, 10, 0, 0, false), PlayerRole.BAT, false)).isEqualTo(13 + 2);
        assertThat(points(batting(7, 10, 0, 0, false), PlayerRole.BAT, false)).isEqualTo(7 - 2);
        assertThat(points(batting(5, 10, 0, 0, false), PlayerRole.BAT, false)).isEqualTo(5 - 4);
        assertThat(points(batting(4, 10, 0, 0, false), PlayerRole.BAT, false)).isEqualTo(4 - 6);
        assertThat(points(batting(4, 9, 0, 0, false), PlayerRole.BAT, false)).as("under 10 balls").isEqualTo(4);
        assertThat(points(batting(4, 10, 0, 0, false), PlayerRole.BOWL, false)).as("bowlers exempt").isEqualTo(4);
    }

    @Test
    void bowlingWicketsBonusesMaidensAndEconomy() {
        // 4 overs, 20 runs (econ 5.00 -> +4), 3 wkts (1 lbw/bowled), 1 maiden, 10 dots
        int expected = 3 * 30 + 8 + 4 + 8 + 10 + 4;
        assertThat(points(bowling(24, 20, 3, 1, 1, 10), PlayerRole.BOWL, false)).isEqualTo(expected);
    }

    @Test
    void economyBrackets() {
        assertThat(calculator.economyPoints(bowling(12, 9, 0, 0, 0, 0))).isEqualTo(6);   // 4.50
        assertThat(calculator.economyPoints(bowling(12, 14, 0, 0, 0, 0))).isEqualTo(2);  // 7.00
        assertThat(calculator.economyPoints(bowling(12, 16, 0, 0, 0, 0))).isZero();      // 8.00
        assertThat(calculator.economyPoints(bowling(12, 22, 0, 0, 0, 0))).isEqualTo(-2); // 11.00
        assertThat(calculator.economyPoints(bowling(12, 24, 0, 0, 0, 0))).isEqualTo(-4); // 12.00
        assertThat(calculator.economyPoints(bowling(12, 25, 0, 0, 0, 0))).isEqualTo(-6); // 12.50
        assertThat(calculator.economyPoints(bowling(11, 30, 0, 0, 0, 0))).as("under two overs").isZero();
    }

    @Test
    void fieldingAndStartingXi() {
        PlayerMatchStats s = new PlayerMatchStats("m", "p");
        s.setCatches(2);
        s.setStumpings(1);
        s.setRunOuts(1);
        assertThat(calculator.calculate(s, PlayerRole.WK, true)).isEqualByComparingTo(BigDecimal.valueOf(16 + 12 + 12 + 4));
    }
}
