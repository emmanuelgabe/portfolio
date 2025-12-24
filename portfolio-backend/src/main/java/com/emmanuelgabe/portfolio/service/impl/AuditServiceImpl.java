package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.AuditContext;
import com.emmanuelgabe.portfolio.audit.AuditEntityType;
import com.emmanuelgabe.portfolio.audit.AuditLogSpecification;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogFilter;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogResponse;
import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.emmanuelgabe.portfolio.entity.AuditLog;
import com.emmanuelgabe.portfolio.exception.AuditExportException;
import com.emmanuelgabe.portfolio.mapper.AuditLogMapper;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.AuditLogRepository;
import com.emmanuelgabe.portfolio.repository.CvRepository;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.SiteConfigurationRepository;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.repository.UserRepository;
import com.emmanuelgabe.portfolio.service.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of AuditService for audit logging operations.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    /**
     * Maximum number of audit logs that can be exported at once.
     * Prevents OutOfMemoryError when exporting large datasets.
     */
    private static final int MAX_EXPORT_LIMIT = 10000;

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    // Entity repositories for state capture
    private final ProjectRepository projectRepository;
    private final ArticleRepository articleRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final TagRepository tagRepository;
    private final CvRepository cvRepository;
    private final SiteConfigurationRepository siteConfigurationRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> captureEntityState(String entityType, Long entityId) {
        log.debug("[AUDIT] Capturing entity state - entityType={}, entityId={}", entityType, entityId);

        Object entity = findEntity(entityType, entityId);
        if (entity == null) {
            log.debug("[AUDIT] Entity not found - entityType={}, entityId={}", entityType, entityId);
            return null;
        }

        return convertToMap(entity);
    }

    /**
     * Sensitive fields that should never be stored in audit logs.
     * Includes password hashes, tokens, and other security-sensitive data.
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwordHash", "hashedPassword",
            "token", "accessToken", "refreshToken",
            "secret", "apiKey", "privateKey",
            "credentials", "authToken"
    );

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.convertValue(obj, Map.class);
            return filterSensitiveFields(map);
        } catch (Exception e) {
            log.warn("[AUDIT] Failed to convert object to map - error={}", e.getMessage());
            return null;
        }
    }

    /**
     * Recursively filters sensitive fields from a map.
     * Replaces sensitive values with "[REDACTED]" for audit trail visibility.
     */
    private Map<String, Object> filterSensitiveFields(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        Map<String, Object> filtered = new HashMap<>(map);
        for (Map.Entry<String, Object> entry : filtered.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (SENSITIVE_FIELDS.stream().anyMatch(sensitive -> key.contains(sensitive.toLowerCase()))) {
                filtered.put(entry.getKey(), "[REDACTED]");
            } else if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                filtered.put(entry.getKey(), filterSensitiveFields(nestedMap));
            }
        }
        return filtered;
    }

    @Override
    public AuditLog createAuditLog(
            AuditContext context,
            AuditAction action,
            String entityType,
            Long entityId,
            String entityName,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            boolean success,
            String errorMessage
    ) {
        log.debug("[AUDIT] Publishing audit event - action={}, entityType={}, entityId={}",
                action, entityType, entityId);

        // Calculate changed fields for UPDATE actions
        List<String> changedFields = null;
        if (oldValues != null && newValues != null) {
            changedFields = calculateChangedFields(oldValues, newValues);
        }

        // Create and publish audit event
        AuditEvent event = AuditEvent.forEntityAudit(
                context,
                action,
                entityType,
                entityId,
                entityName,
                oldValues,
                newValues,
                changedFields,
                success,
                errorMessage
        );

        eventPublisher.publishAuditEvent(event);
        log.debug("[AUDIT] Audit event published - eventId={}, action={}, entityType={}",
                event.getEventId(), action, entityType);

        // Return null since audit is now async
        return null;
    }

    @Override
    public void logAuthEvent(
            AuditAction action,
            String username,
            String ipAddress,
            String userAgent,
            boolean success,
            String errorMessage
    ) {
        log.debug("[AUDIT] Publishing auth event - action={}, username={}, success={}",
                action, username, success);

        // Create and publish auth event
        AuditEvent event = AuditEvent.forAuthEvent(
                action,
                username,
                ipAddress,
                userAgent,
                success,
                errorMessage
        );

        eventPublisher.publishAuditEvent(event);
        log.debug("[AUDIT] Auth event published - eventId={}, action={}, username={}",
                event.getEventId(), action, username);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(AuditLogFilter filter, Pageable pageable) {
        log.debug("[AUDIT] Fetching audit logs with filter");

        Specification<AuditLog> spec = AuditLogSpecification.withFilter(filter);
        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);

        return page.map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityHistory(String entityType, Long entityId) {
        log.debug("[AUDIT] Fetching entity history - entityType={}, entityId={}", entityType, entityId);

        List<AuditLog> history = auditLogRepository.findEntityHistory(entityType, entityId);
        return history.stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditStatsResponse getStats(LocalDateTime since) {
        log.debug("[AUDIT] Calculating stats - since={}", since);

        AuditStatsResponse stats = new AuditStatsResponse();

        // Action counts
        Map<String, Long> actionCounts = new HashMap<>();
        auditLogRepository.countByActionSince(since).forEach(row ->
                actionCounts.put(((AuditAction) row[0]).name(), (Long) row[1]));
        stats.setActionCounts(actionCounts);

        // Entity type counts
        Map<String, Long> entityCounts = new HashMap<>();
        auditLogRepository.countByEntityTypeSince(since).forEach(row ->
                entityCounts.put((String) row[0], (Long) row[1]));
        stats.setEntityTypeCounts(entityCounts);

        // Daily activity
        Map<String, Long> dailyActivity = new LinkedHashMap<>();
        auditLogRepository.countByDateSince(since).forEach(row ->
                dailyActivity.put(row[0].toString(), (Long) row[1]));
        stats.setDailyActivity(dailyActivity);

        // User activity
        Map<String, Long> userActivity = new LinkedHashMap<>();
        auditLogRepository.countByUserSince(since).forEach(row ->
                userActivity.put((String) row[0], (Long) row[1]));
        stats.setUserActivity(userActivity);

        // Combined total and failed counts (reduces N+1: 2 queries -> 1)
        Object[] totalAndFailed = auditLogRepository.countTotalAndFailedSince(since);
        stats.setTotalCount(totalAndFailed[0] != null ? ((Number) totalAndFailed[0]).longValue() : 0L);
        stats.setFailedActionsCount(totalAndFailed[1] != null ? ((Number) totalAndFailed[1]).longValue() : 0L);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToCsv(AuditLogFilter filter) {
        log.info("[AUDIT] Exporting audit logs to CSV");

        List<AuditLog> logs = fetchLogsForExport(filter);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {
            // BOM for Excel UTF-8 compatibility
            writer.print('\ufeff');

            // Header
            writer.println("ID,Timestamp,Action,Entity Type,Entity ID,Entity Name,"
                    + "Username,IP Address,Success,Error Message");

            DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            for (AuditLog auditLog : logs) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        auditLog.getId(),
                        auditLog.getCreatedAt().format(dtf),
                        auditLog.getAction(),
                        auditLog.getEntityType(),
                        auditLog.getEntityId() != null ? auditLog.getEntityId() : "",
                        escapeCsv(auditLog.getEntityName()),
                        auditLog.getUsername(),
                        auditLog.getIpAddress() != null ? auditLog.getIpAddress() : "",
                        auditLog.isSuccess(),
                        escapeCsv(auditLog.getErrorMessage())
                );
            }
        }

        log.info("[AUDIT] Exported {} audit logs to CSV", logs.size());
        return baos.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToJson(AuditLogFilter filter) {
        log.info("[AUDIT] Exporting audit logs to JSON");

        List<AuditLog> logs = fetchLogsForExport(filter);
        List<AuditLogResponse> responses = logs.stream()
                .map(auditLogMapper::toResponse)
                .toList();

        try {
            byte[] result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(responses);
            log.info("[AUDIT] Exported {} audit logs to JSON", logs.size());
            return result;
        } catch (JsonProcessingException e) {
            log.error("[AUDIT] Failed to export audit logs to JSON - error={}", e.getMessage());
            throw new AuditExportException("Failed to export audit logs to JSON", e);
        }
    }

    /**
     * Fetch audit logs for export with limit to prevent OutOfMemoryError.
     *
     * @param filter the filter criteria
     * @return list of audit logs (max MAX_EXPORT_LIMIT entries)
     */
    private List<AuditLog> fetchLogsForExport(AuditLogFilter filter) {
        Specification<AuditLog> spec = AuditLogSpecification.withFilter(filter);
        Pageable pageable = PageRequest.of(0, MAX_EXPORT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);

        if (page.getTotalElements() > MAX_EXPORT_LIMIT) {
            log.warn("[AUDIT] Export limited to {} entries out of {} total",
                    MAX_EXPORT_LIMIT, page.getTotalElements());
        }

        return page.getContent();
    }

    /**
     * Find entity by type and ID using AuditEntityType enum.
     */
    private Object findEntity(String entityType, Long entityId) {
        AuditEntityType type = AuditEntityType.fromName(entityType);
        if (type == null) {
            log.debug("[AUDIT] Unknown entity type - entityType={}", entityType);
            return null;
        }

        return switch (type) {
            case PROJECT -> projectRepository.findById(entityId).orElse(null);
            case ARTICLE -> articleRepository.findById(entityId).orElse(null);
            case SKILL -> skillRepository.findById(entityId).orElse(null);
            case EXPERIENCE -> experienceRepository.findById(entityId).orElse(null);
            case TAG -> tagRepository.findById(entityId).orElse(null);
            case CV -> cvRepository.findById(entityId).orElse(null);
            case SITE_CONFIGURATION -> siteConfigurationRepository.findById(entityId).orElse(null);
            case USER -> userRepository.findById(entityId).orElse(null);
        };
    }

    /**
     * Calculate which fields changed between old and new values.
     */
    private List<String> calculateChangedFields(Map<String, Object> oldValues,
                                                Map<String, Object> newValues) {
        List<String> changedFields = new ArrayList<>();
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(oldValues.keySet());
        allKeys.addAll(newValues.keySet());

        for (String key : allKeys) {
            Object oldVal = oldValues.get(key);
            Object newVal = newValues.get(key);
            if (!Objects.equals(oldVal, newVal)) {
                changedFields.add(key);
            }
        }

        return changedFields;
    }

    /**
     * Escape value for CSV format.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog persistAuditEvent(AuditEvent event) {
        log.debug("[AUDIT] Persisting audit event - eventId={}, action={}, entityType={}",
                event.getEventId(), event.getAction(), event.getEntityType());

        AuditLog auditLog = auditLogMapper.toEntity(event);

        // Handle user relationship for auth events
        if (event.getUserId() != null) {
            userRepository.findById(event.getUserId())
                    .ifPresent(auditLog::setUser);
        } else if (event.getAuditType() == AuditEvent.AuditEventType.AUTH_EVENT
                && event.isSuccess()
                && event.getAction() == AuditAction.LOGIN
                && event.getUsername() != null) {
            userRepository.findByUsername(event.getUsername()).ifPresent(user -> {
                auditLog.setUser(user);
                auditLog.setUserRole(user.getRole().name());
                auditLog.setEntityId(user.getId());
            });
        }

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("[AUDIT] Audit event persisted - eventId={}, auditLogId={}",
                event.getEventId(), saved.getId());

        return saved;
    }
}
