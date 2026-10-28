package com.ipld11stars.backend.model;

/**
 * Extras that can be recorded on a delivery.
 * Wides and no-balls are illegal deliveries and their runs are charged to the bowler;
 * byes and leg-byes come off a legal delivery and are charged to neither batter nor bowler.
 */
public enum ExtrasType {
    NONE,
    WIDE,
    NO_BALL,
    BYE,
    LEG_BYE;

    public boolean isLegalDelivery() {
        return this != WIDE && this != NO_BALL;
    }

    public boolean isChargedToBowler() {
        return this == WIDE || this == NO_BALL;
    }
}
