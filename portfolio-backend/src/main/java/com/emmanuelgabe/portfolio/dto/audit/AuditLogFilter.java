package com.emmanuelgabe.portfolio.dto.audit;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Filter DTO for querying audit logs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilter {

    private AuditAction action;
    private String entityType;
    private Long entityId;
    private String username;
    private Boolean success;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
