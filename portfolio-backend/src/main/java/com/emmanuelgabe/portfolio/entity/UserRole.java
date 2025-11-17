package com.emmanuelgabe.portfolio.entity;

/**
 * User roles for authorization (Spring Security convention with ROLE_ prefix)
 * ROLE_ADMIN: Full access to all resources and admin panel
 * ROLE_GUEST: Read-only access to public content
 */
public enum UserRole {
    /**
     * Administrator with full access
     */
    ROLE_ADMIN,

    /**
     * Guest user with read-only access
     */
    ROLE_GUEST
}
