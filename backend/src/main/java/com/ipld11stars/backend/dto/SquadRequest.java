package com.ipld11stars.backend.dto;

import java.util.List;

/**
 * Body of PUT /matches/{matchId}/squad: replaces one team's squad for the match.
 */
public record SquadRequest(String teamId, List<String> playerIds, List<String> startingXi) {
}
