package com.ipld11stars.backend.dto;

import java.util.List;

/**
 * Body of POST /contests/{contestId}/entries: the 11 players plus captain and vice-captain.
 */
public record JoinContestRequest(String entryName, List<String> playerIds, String captainId, String viceCaptainId) {
}
