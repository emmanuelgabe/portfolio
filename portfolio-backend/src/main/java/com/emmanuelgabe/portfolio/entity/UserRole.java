package com.emmanuelgabe.portfolio.entity;

/**
 * User roles for authorization (Spring Security convention with ROLE_ prefix)
 * ROLE_ADMIN: Full access to all resources and admin panel
 * ROLE_GUEST: Read-only access to public content
 */
public enum UserRole {
    ROLE_ADMIN,
    ROLE_GUEST
}
