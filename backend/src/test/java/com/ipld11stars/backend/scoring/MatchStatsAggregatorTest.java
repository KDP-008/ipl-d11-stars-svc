package com.ipld11stars.backend.scoring;

import com.ipld11stars.backend.model.Delivery;
import com.ipld11stars.backend.model.DismissalType;
import com.ipld11stars.backend.model.ExtrasType;
import com.ipld11stars.backend.model.PlayerMatchStats;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MatchStatsAggregatorTest {

    private final MatchStatsAggregator aggregator = new MatchStatsAggregator();
    private int seq = 0;

    private Delivery d(int over, String striker, String bowler, int runs, ExtrasType extras, int extrasRuns) {
        seq++;
        return new Delivery("d" + seq, "m", 1, seq, over, 1, "BAT", "BOWL", striker, "ns", bowler, runs,
                extras, extrasRuns, extras.isLegalDelivery(), false, false, null, null, null, null);
    }

    private Delivery out(int over, String striker, String bowler, DismissalType type, String dismissed, String fielder) {
        seq++;
        return new Delivery("d" + seq, "m", 1, seq, over, 1, "BAT", "BOWL", striker, "ns", bowler, 0,
                ExtrasType.NONE, 0, true, false, true, type, dismissed, fielder, null);
    }

    @Test
    void extrasAreChargedAndCountedCorrectly() {
        List<Delivery> list = List.of(
                d(1, "s", "b", 0, ExtrasType.WIDE, 1),     // not a ball faced, 1 conceded, not legal
                d(1, "s", "b", 4, ExtrasType.NO_BALL, 1),  // ball faced, 4 to batter, 5 conceded, not legal
                d(1, "s", "b", 0, ExtrasType.LEG_BYE, 2),  // ball faced, nothing conceded, legal dot for bowler
                d(1, "s", "b", 0, ExtrasType.BYE, 4),      // ball faced, nothing conceded, legal dot
                d(1, "s", "b", 1, ExtrasType.NONE, 0));
        Map<String, PlayerMatchStats> stats = aggregator.aggregate("m", list);

        PlayerMatchStats striker = stats.get("s");
        assertThat(striker.getRunsScored()).isEqualTo(5);
        assertThat(striker.getBallsFaced()).isEqualTo(4);
        assertThat(striker.getFours()).isEqualTo(1);

        PlayerMatchStats bowler = stats.get("b");
        assertThat(bowler.getBallsBowled()).isEqualTo(3);
        assertThat(bowler.getRunsConceded()).isEqualTo(7);
        assertThat(bowler.getDotBalls()).isEqualTo(2);
    }

    @Test
    void maidenNeedsSixLegalBallsWithNothingConcededByTheBowler() {
        List<Delivery> maiden = new ArrayList<>();
        for (int i = 0; i < 5; i++) maiden.add(d(1, "s", "b1", 0, ExtrasType.NONE, 0));
        maiden.add(d(1, "s", "b1", 0, ExtrasType.LEG_BYE, 1)); // leg-bye does not spoil a maiden
        for (int i = 0; i < 6; i++) maiden.add(d(2, "s", "b2", 0, ExtrasType.NONE, 0));
        maiden.add(2 + 5, d(2, "s", "b2", 0, ExtrasType.WIDE, 1)); // a wide in over 2 spoils it
        Map<String, PlayerMatchStats> stats = aggregator.aggregate("m", maiden);
        assertThat(stats.get("b1").getMaidens()).isEqualTo(1);
        assertThat(stats.get("b2").getMaidens()).isZero();
    }

    @Test
    void wicketsAreCreditedToBowlerAndFielder() {
        List<Delivery> list = List.of(
                out(1, "s1", "b", DismissalType.LBW, "s1", null),
                out(1, "s2", "b", DismissalType.CAUGHT, "s2", "f1"),
                out(1, "s3", "b", DismissalType.RUN_OUT, "s3", "f2"),
                out(1, "s4", "b", DismissalType.STUMPED, "s4", "wk"));
        Map<String, PlayerMatchStats> stats = aggregator.aggregate("m", list);
        assertThat(stats.get("b").getWickets()).isEqualTo(3);
        assertThat(stats.get("b").getLbwBowled()).isEqualTo(1);
        assertThat(stats.get("f1").getCatches()).isEqualTo(1);
        assertThat(stats.get("f2").getRunOuts()).isEqualTo(1);
        assertThat(stats.get("wk").getStumpings()).isEqualTo(1);
        assertThat(stats.get("s3").isDismissed()).isTrue();
    }
}
