package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.AuditEntityType;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogFilter;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogResponse;
import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.emmanuelgabe.portfolio.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for audit log management.
 * Provides endpoints for viewing, filtering, and exporting audit logs.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminAuditController {

    private final AuditService auditService;

    /**
     * Get paginated audit logs with optional filtering.
     *
     * @param action filter by action type
     * @param entityType filter by entity type
     * @param entityId filter by entity ID
     * @param username filter by username
     * @param success filter by success status
     * @param startDate filter by start date
     * @param endDate filter by end date
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.debug("[AUDIT_API] Fetching audit logs - action={}, entityType={}", action, entityType);

        AuditLogFilter filter = new AuditLogFilter();
        filter.setAction(action);
        filter.setEntityType(entityType);
        filter.setEntityId(entityId);
        filter.setUsername(username);
        filter.setSuccess(success);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        Page<AuditLogResponse> page = auditService.getAuditLogs(filter, pageable);
        log.debug("[AUDIT_API] Found {} audit logs", page.getTotalElements());

        return ResponseEntity.ok(page);
    }

    /**
     * Get audit history for a specific entity.
     *
     * @param entityType the entity type
     * @param entityId the entity ID
     * @return list of audit logs for the entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogResponse>> getEntityHistory(
            @PathVariable @NotBlank String entityType,
            @PathVariable @Min(1) Long entityId
    ) {
        log.debug("[AUDIT_API] Fetching entity history - entityType={}, entityId={}",
                entityType, entityId);

        List<AuditLogResponse> history = auditService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get audit statistics for dashboard.
     *
     * @param days number of days to include in stats (default 30, max 365)
     * @return statistics response
     */
    @GetMapping("/stats")
    public ResponseEntity<AuditStatsResponse> getStats(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        log.debug("[AUDIT_API] Fetching stats for last {} days", days);

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        AuditStatsResponse stats = auditService.getStats(since);
        return ResponseEntity.ok(stats);
    }

    /**
     * Export audit logs to CSV format.
     *
     * @param action filter by action type
     * @param entityType filter by entity type
     * @param startDate filter by start date
     * @param endDate filter by end date
     * @return CSV file
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCsv(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate
    ) {
        log.info("[AUDIT_API] Exporting audit logs to CSV");

        AuditLogFilter filter = new AuditLogFilter();
        filter.setAction(action);
        filter.setEntityType(entityType);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        byte[] csv = auditService.exportToCsv(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    /**
     * Export audit logs to JSON format.
     *
     * @param action filter by action type
     * @param entityType filter by entity type
     * @param startDate filter by start date
     * @param endDate filter by end date
     * @return JSON file
     */
    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportToJson(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate
    ) {
        log.info("[AUDIT_API] Exporting audit logs to JSON");

        AuditLogFilter filter = new AuditLogFilter();
        filter.setAction(action);
        filter.setEntityType(entityType);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        byte[] json = auditService.exportToJson(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    /**
     * Get available actions for filtering.
     *
     * @return array of all audit actions
     */
    @GetMapping("/actions")
    public ResponseEntity<AuditAction[]> getActions() {
        return ResponseEntity.ok(AuditAction.values());
    }

    /**
     * Get available entity types for filtering.
     *
     * @return list of entity type names
     */
    @GetMapping("/entity-types")
    public ResponseEntity<List<String>> getEntityTypes() {
        return ResponseEntity.ok(AuditEntityType.getAllNames());
    }
}
