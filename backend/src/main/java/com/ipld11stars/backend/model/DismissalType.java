package com.ipld11stars.backend.model;

/**
 * Ways a batter can be dismissed. Every mode except RUN_OUT is credited to the bowler.
 */
public enum DismissalType {
    BOWLED,
    CAUGHT,
    LBW,
    STUMPED,
    HIT_WICKET,
    RUN_OUT;

    public boolean isCreditedToBowler() {
        return this != RUN_OUT;
    }

    public boolean isBowledOrLbw() {
        return this == BOWLED || this == LBW;
    }

    public boolean requiresFielder() {
        return this == CAUGHT || this == STUMPED || this == RUN_OUT;
    }
}
