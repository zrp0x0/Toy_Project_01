package com.zrp.toyproject01.domain.account.domain;

public enum Role {
    ROLE_USER("user"),
    ROLE_ADMIN("admin");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
