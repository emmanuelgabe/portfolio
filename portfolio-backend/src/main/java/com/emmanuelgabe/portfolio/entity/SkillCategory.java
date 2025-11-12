package com.emmanuelgabe.portfolio.entity;

public enum SkillCategory {
    FRONTEND("Frontend"),
    BACKEND("Backend"),
    DATABASE("Database"),
    DEVOPS("DevOps"),
    TOOLS("Tools");

    private final String displayName;

    SkillCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
