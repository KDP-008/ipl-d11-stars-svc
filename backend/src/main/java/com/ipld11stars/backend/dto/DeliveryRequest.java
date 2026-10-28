package com.ipld11stars.backend.dto;

import com.ipld11stars.backend.model.DismissalType;
import com.ipld11stars.backend.model.ExtrasType;

/**
 * Body of POST /matches/{matchId}/deliveries. One request per ball, in the order they were bowled.
 */
public record DeliveryRequest(
        Integer innings,
        Integer overNumber,
        Integer ballNumber,
        String battingTeamId,
        String strikerId,
        String nonStrikerId,
        String bowlerId,
        Integer runsBatter,
        ExtrasType extrasType,
        Integer extrasRuns,
        Wicket wicket
) {
    public record Wicket(DismissalType dismissalType, String dismissedPlayerId, String fielderId) {
    }

    public int runsBatterOrZero() {
        return runsBatter != null ? runsBatter : 0;
    }

    public int extrasRunsOrZero() {
        return extrasRuns != null ? extrasRuns : 0;
    }

    public ExtrasType extrasTypeOrNone() {
        return extrasType != null ? extrasType : ExtrasType.NONE;
    }
}
