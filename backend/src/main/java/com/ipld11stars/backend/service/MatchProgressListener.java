package com.ipld11stars.backend.service;

/**
 * Hook for components that react to live scoring, e.g. closing contests and refreshing leaderboards.
 * Called inside the transaction that recorded the delivery.
 */
public interface MatchProgressListener {

    /** Called before the first delivery of a match is stored. */
    default void onMatchStarting(String matchId) {
    }

    /** Called after a delivery is stored and player stats have been recomputed. */
    default void onDeliveryRecorded(String matchId) {
    }
}
