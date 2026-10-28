package com.ipld11stars.backend.model;

import java.math.BigDecimal;

/**
 * A player from the players table, as needed for team selection and scoring.
 */
public record Player(String id, String teamId, String fullName, PlayerRole role, BigDecimal credits) {
}
