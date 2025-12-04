package com.emmanuelgabe.portfolio.audit;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeration of entity types that can be audited.
 * Centralizes entity type names to avoid duplication.
 */
public enum AuditEntityType {

    PROJECT("Project"),
    ARTICLE("Article"),
    SKILL("Skill"),
    EXPERIENCE("Experience"),
    TAG("Tag"),
    CV("Cv"),
    SITE_CONFIGURATION("SiteConfiguration"),
    USER("User");

    private final String name;

    AuditEntityType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Get all entity type names as a list.
     *
     * @return list of entity type names
     */
    public static List<String> getAllNames() {
        return Arrays.stream(values())
                .map(AuditEntityType::getName)
                .toList();
    }

    /**
     * Find entity type by name.
     *
     * @param name the entity type name
     * @return the matching AuditEntityType or null if not found
     */
    public static AuditEntityType fromName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
