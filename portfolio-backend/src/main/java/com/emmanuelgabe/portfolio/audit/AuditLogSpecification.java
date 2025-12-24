package com.emmanuelgabe.portfolio.audit;

import com.emmanuelgabe.portfolio.dto.audit.AuditLogFilter;
import com.emmanuelgabe.portfolio.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * JPA Specification for dynamic AuditLog filtering.
 */
public final class AuditLogSpecification {

    private AuditLogSpecification() {
        // Utility class
    }

    /**
     * Creates a combined specification from the filter.
     *
     * @param filter the filter criteria
     * @return the combined specification
     */
    public static Specification<AuditLog> withFilter(AuditLogFilter filter) {
        return Specification.where(hasAction(filter.getAction()))
                .and(hasEntityType(filter.getEntityType()))
                .and(hasEntityId(filter.getEntityId()))
                .and(hasUsername(filter.getUsername()))
                .and(hasSuccess(filter.getSuccess()))
                .and(createdBetween(filter.getStartDate(), filter.getEndDate()));
    }

    /**
     * Filter by action type.
     */
    private static Specification<AuditLog> hasAction(AuditAction action) {
        return (root, query, cb) -> {
            if (action == null) {
                return null;
            }
            return cb.equal(root.get("action"), action);
        };
    }

    /**
     * Filter by entity type.
     */
    private static Specification<AuditLog> hasEntityType(String entityType) {
        return (root, query, cb) -> {
            if (entityType == null || entityType.isBlank()) {
                return null;
            }
            return cb.equal(root.get("entityType"), entityType);
        };
    }

    /**
     * Filter by entity ID.
     */
    private static Specification<AuditLog> hasEntityId(Long entityId) {
        return (root, query, cb) -> {
            if (entityId == null) {
                return null;
            }
            return cb.equal(root.get("entityId"), entityId);
        };
    }

    /**
     * Filter by username (case-insensitive partial match).
     */
    private static Specification<AuditLog> hasUsername(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("username")),
                    "%" + username.toLowerCase() + "%");
        };
    }

    /**
     * Filter by success status.
     */
    private static Specification<AuditLog> hasSuccess(Boolean success) {
        return (root, query, cb) -> {
            if (success == null) {
                return null;
            }
            return cb.equal(root.get("success"), success);
        };
    }

    /**
     * Filter by creation date range.
     */
    private static Specification<AuditLog> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return null;
            }
            if (start != null && end != null) {
                return cb.between(root.get("createdAt"), start, end);
            }
            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }
}
