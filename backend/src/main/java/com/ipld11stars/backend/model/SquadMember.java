package com.ipld11stars.backend.model;

import java.math.BigDecimal;

/**
 * A player named in a match squad, joined with their player record.
 */
public record SquadMember(String matchId, String playerId, String teamId, String fullName,
                          PlayerRole role, BigDecimal credits, boolean startingXi) {
}
