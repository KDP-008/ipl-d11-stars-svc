package com.ipld11stars.backend.model;

import java.time.LocalDateTime;

/**
 * One recorded ball (legal or not) in a match innings.
 * sequenceNo orders deliveries within an innings, including wides and no-balls.
 */
public record Delivery(
        String id,
        String matchId,
        int innings,
        int sequenceNo,
        int overNumber,
        int ballNumber,
        String battingTeamId,
        String bowlingTeamId,
        String strikerId,
        String nonStrikerId,
        String bowlerId,
        int runsBatter,
        ExtrasType extrasType,
        int extrasRuns,
        boolean legal,
        boolean freeHit,
        boolean wicket,
        DismissalType dismissalType,
        String dismissedPlayerId,
        String fielderId,
        LocalDateTime createdAt
) {
    public int totalRuns() {
        return runsBatter + extrasRuns;
    }

    /** Runs that count against the bowler's figures: runs off the bat plus wides and no-balls. */
    public int runsConcededByBowler() {
        return runsBatter + (extrasType.isChargedToBowler() ? extrasRuns : 0);
    }
}
