package com.ipld11stars.backend.scoring;

import com.ipld11stars.backend.model.Delivery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.ipld11stars.backend.scoring.ScoringRules.*;

/**
 * Running state of one innings, rebuilt by replaying its recorded deliveries in order.
 * Used to validate the next delivery against the laws we enforce.
 */
public class InningsState {

    private final int innings;
    private String battingTeamId;
    private String bowlingTeamId;
    private int runs;
    private int wickets;
    private int legalBalls;
    private int deliveries;
    private boolean freeHitPending;
    private String currentOverBowlerId;
    private String previousOverBowlerId;
    private final Map<String, Integer> legalBallsByBowler = new HashMap<>();
    private final Set<String> dismissedBatters = new HashSet<>();

    public InningsState(int innings) {
        this.innings = innings;
    }

    public void apply(Delivery d) {
        if (deliveries == 0) {
            battingTeamId = d.battingTeamId();
            bowlingTeamId = d.bowlingTeamId();
        }
        deliveries++;
        runs += d.totalRuns();
        currentOverBowlerId = d.bowlerId();
        if (d.wicket()) {
            wickets++;
            dismissedBatters.add(d.dismissedPlayerId());
        }
        if (d.extrasType() == com.ipld11stars.backend.model.ExtrasType.NO_BALL) {
            freeHitPending = true;
        } else if (d.legal()) {
            freeHitPending = false;
        }
        if (d.legal()) {
            legalBalls++;
            legalBallsByBowler.merge(d.bowlerId(), 1, Integer::sum);
            if (legalBalls % BALLS_PER_OVER == 0) {
                previousOverBowlerId = currentOverBowlerId;
                currentOverBowlerId = null;
            }
        }
    }

    /** True once the innings can take no more deliveries. {@code target} is null for the first innings. */
    public boolean isComplete(Integer target) {
        return wickets >= MAX_WICKETS
                || legalBalls >= MAX_OVERS * BALLS_PER_OVER
                || (target != null && runs >= target);
    }

    /** The over the next delivery belongs to (1-based). */
    public int currentOverNumber() {
        return legalBalls / BALLS_PER_OVER + 1;
    }

    public boolean isOverInProgress() {
        return currentOverBowlerId != null;
    }

    public int oversBowledBy(String bowlerId) {
        return legalBallsByBowler.getOrDefault(bowlerId, 0) / BALLS_PER_OVER;
    }

    public String scoreLine() {
        return runs + "/" + wickets + " (" + (legalBalls / BALLS_PER_OVER) + "." + (legalBalls % BALLS_PER_OVER) + " ov)";
    }

    public int getInnings() { return innings; }
    public String getBattingTeamId() { return battingTeamId; }
    public String getBowlingTeamId() { return bowlingTeamId; }
    public int getRuns() { return runs; }
    public int getWickets() { return wickets; }
    public int getLegalBalls() { return legalBalls; }
    public int getDeliveries() { return deliveries; }
    public boolean isFreeHitPending() { return freeHitPending; }
    public String getCurrentOverBowlerId() { return currentOverBowlerId; }
    public String getPreviousOverBowlerId() { return previousOverBowlerId; }
    public boolean hasStarted() { return deliveries > 0; }
    public boolean isDismissed(String playerId) { return dismissedBatters.contains(playerId); }
}
