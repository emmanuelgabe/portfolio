package com.emmanuelgabe.portfolio.audit;

/**
 * Enumeration of all auditable actions in the system.
 * Grouped by category for clarity and dashboard filtering.
 */
public enum AuditAction {

    // Entity CRUD
    CREATE("Entity created"),
    UPDATE("Entity updated"),
    DELETE("Entity deleted"),

    // Article-specific
    PUBLISH("Article published"),
    UNPUBLISH("Article unpublished"),

    // Project-specific
    FEATURE("Project featured"),
    UNFEATURE("Project unfeatured"),

    // CV-specific
    SET_CURRENT("CV set as current"),

    // Authentication
    LOGIN("User logged in"),
    LOGOUT("User logged out"),
    LOGIN_FAILED("Login attempt failed"),
    PASSWORD_CHANGE("Password changed"),

    // AI Health Monitor
    PROPOSE("Health action proposed by agent"),
    APPROVE("Health action approved by admin"),
    REJECT("Health action rejected by admin"),
    EXECUTE("Health action executed"),
    EXPIRE("Health action expired");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
