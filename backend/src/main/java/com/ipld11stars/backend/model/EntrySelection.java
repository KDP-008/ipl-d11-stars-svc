package com.ipld11stars.backend.model;

/**
 * One player picked in a contest entry, with the captain (2x) and vice-captain (1.5x) flags.
 */
public record EntrySelection(String entryId, String playerId, boolean captain, boolean viceCaptain) {
}
