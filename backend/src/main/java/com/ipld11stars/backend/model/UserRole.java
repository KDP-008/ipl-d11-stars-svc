package com.ipld11stars.backend.model;

public enum UserRole {
    ADMIN("ADMIN"),
    CONTEST_CREATOR("CONTEST_CREATOR"),
    PLAYER("PLAYER");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromValue(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return PLAYER;
    }
}
