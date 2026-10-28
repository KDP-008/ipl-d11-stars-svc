package com.ipld11stars.backend.model;

/**
 * Fantasy roles as stored in players.role, with the team-selection limits per role.
 */
public enum PlayerRole {
    WK(1, 4),
    BAT(3, 6),
    AR(1, 4),
    BOWL(3, 6);

    private final int minPerTeam;
    private final int maxPerTeam;

    PlayerRole(int minPerTeam, int maxPerTeam) {
        this.minPerTeam = minPerTeam;
        this.maxPerTeam = maxPerTeam;
    }

    public int getMinPerTeam() {
        return minPerTeam;
    }

    public int getMaxPerTeam() {
        return maxPerTeam;
    }

    public static PlayerRole fromCode(String code) {
        if (code == null) {
            return BAT;
        }
        return PlayerRole.valueOf(code.trim().toUpperCase());
    }
}
