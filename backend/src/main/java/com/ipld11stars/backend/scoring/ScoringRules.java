package com.ipld11stars.backend.scoring;

/**
 * Fantasy points table for T20 matches.
 * Base values mirror the web client's scoring-engine.ts; strike-rate and economy adjustments
 * follow the usual T20 fantasy brackets.
 */
public final class ScoringRules {

    private ScoringRules() {
    }

    // Batting
    public static final int RUN = 1;
    public static final int BOUNDARY_BONUS = 4;
    public static final int SIX_BONUS = 6;
    public static final int THIRTY_RUN_BONUS = 4;
    public static final int HALF_CENTURY_BONUS = 8;
    public static final int CENTURY_BONUS = 16;
    public static final int DUCK = -2;

    // Bowling
    public static final int WICKET = 30;
    public static final int LBW_BOWLED_BONUS = 8;
    public static final int THREE_WICKET_BONUS = 4;
    public static final int MAIDEN = 8;
    public static final int DOT_BALL = 1;

    // Fielding
    public static final int CATCH = 8;
    public static final int STUMPING = 12;
    public static final int RUN_OUT = 12;

    // Other
    public static final int STARTING_XI = 4;

    /** Strike-rate adjustment needs this many balls faced and never applies to bowlers. */
    public static final int STRIKE_RATE_MIN_BALLS = 10;
    /** Economy adjustment needs at least two overs bowled. */
    public static final int ECONOMY_MIN_BALLS = 12;

    // T20 match shape
    public static final int BALLS_PER_OVER = 6;
    public static final int MAX_OVERS = 20;
    public static final int MAX_OVERS_PER_BOWLER = 4;
    public static final int MAX_WICKETS = 10;
}
