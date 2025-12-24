package com.emmanuelgabe.portfolio.dto.audit;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for audit log entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private String actionDescription;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String username;
    private String userRole;
    private String ipAddress;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private List<String> changedFields;
    private String requestMethod;
    private String requestUri;
    private boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
}
