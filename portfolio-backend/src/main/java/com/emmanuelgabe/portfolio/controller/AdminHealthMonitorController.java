package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.AuditContextHolder;
import com.emmanuelgabe.portfolio.audit.AuditEntityType;
import com.emmanuelgabe.portfolio.dto.health.ActionDecisionRequest;
import com.emmanuelgabe.portfolio.dto.health.HealthReportResponse;
import com.emmanuelgabe.portfolio.dto.health.ProposedActionResponse;
import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.emmanuelgabe.portfolio.healthmonitor.HealthAgentService;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorEventPublisher;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorMapper;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorMetrics;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorProperties;
import com.emmanuelgabe.portfolio.healthmonitor.HealthPreCheckService;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionLevel;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;
import com.emmanuelgabe.portfolio.healthmonitor.executor.ActionExecutionResult;
import com.emmanuelgabe.portfolio.healthmonitor.executor.ActionExecutor;
import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import com.emmanuelgabe.portfolio.repository.HealthReportRepository;
import com.emmanuelgabe.portfolio.service.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin REST API for the AI Health Monitor agent.
 *
 * <p>All endpoints are mapped under {@code /api/admin/health-monitor/**} and
 * thus protected by the {@code hasRole("ADMIN")} rule in {@code SecurityConfig}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/health-monitor")
@RequiredArgsConstructor
public class AdminHealthMonitorController {

    private static final ObjectMapper RESULT_MAPPER = new ObjectMapper();

    private final HealthAgentService agentService;
    private final ObjectProvider<ActionExecutor> actionExecutorProvider;
    private final ObjectProvider<HealthPreCheckService> preCheckServiceProvider;
    private final HealthReportRepository reportRepository;
    private final HealthProposedActionRepository actionRepository;
    private final HealthMonitorMapper mapper;
    private final HealthMonitorEventPublisher eventPublisher;
    private final HealthMonitorMetrics metrics;
    private final HealthMonitorProperties properties;
    private final AuditService auditService;

    // ========== Reports ==========

    @GetMapping("/reports")
    public ResponseEntity<List<HealthReportResponse>> listReports(
            @RequestParam(defaultValue = "20") int limit) {
        int capped = Math.min(Math.max(limit, 1), 100);
        List<HealthReportResponse> reports = reportRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, capped, Sort.unsorted()))
                .map(mapper::toResponse)
                .getContent();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<HealthReportResponse> getReport(@PathVariable Long id) {
        return reportRepository.findById(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/analyze-now")
    public ResponseEntity<HealthReportResponse> analyzeNow() {
        log.info("[HEALTH_MONITOR] Admin-triggered on-demand analysis - bypassPrecheck={}",
                properties.onDemandBypassPrecheck());

        // When bypass is disabled, the admin still wants an on-demand cycle but
        // only if a cheap pre-check signals an anomaly. This saves the Gemini
        // budget on clicks that would otherwise all end up in a GREEN report.
        if (!properties.onDemandBypassPrecheck()) {
            HealthPreCheckService preCheck = preCheckServiceProvider.getIfAvailable();
            if (preCheck != null) {
                HealthPreCheckService.PreCheckResult result = preCheck.run();
                if (!result.anomalyDetected()) {
                    log.info("[HEALTH_MONITOR] Pre-check clean on analyze-now - skipping LLM");
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                }
                log.info("[HEALTH_MONITOR] Pre-check flagged anomaly - reason={}", result.reason());
            }
        }

        HealthReport report = agentService.analyze(HealthCycleType.ON_DEMAND);
        HealthReportResponse response = mapper.toResponse(report);
        eventPublisher.publishNewReport(response);
        try {
            auditService.createAuditLog(
                    AuditContextHolder.getContext(),
                    AuditAction.CREATE,
                    AuditEntityType.HEALTH_REPORT.getName(),
                    report.getId(),
                    report.getSeverity().name(),
                    Collections.emptyMap(),
                    Map.of("cycleType", report.getCycleType().name()),
                    true, null);
        } catch (Exception e) {
            log.warn("[HEALTH_MONITOR] Audit log failed - reportId={}, error={}",
                    report.getId(), e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ========== Actions ==========

    @GetMapping("/actions/pending")
    public ResponseEntity<List<ProposedActionResponse>> listPending() {
        List<ProposedActionResponse> pending = actionRepository
                .findByStatusOrderByCreatedAtDesc(ActionStatus.PENDING)
                .stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/actions")
    public ResponseEntity<List<ProposedActionResponse>> listActions(
            @RequestParam(defaultValue = "50") int limit) {
        int capped = Math.min(Math.max(limit, 1), 200);
        List<ProposedActionResponse> actions = actionRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, capped))
                .map(mapper::toResponse)
                .getContent();
        return ResponseEntity.ok(actions);
    }

    @PostMapping("/actions/{id}/approve")
    @Transactional
    public ResponseEntity<ProposedActionResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid ActionDecisionRequest request) {

        Optional<HealthProposedAction> maybe = actionRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        HealthProposedAction action = maybe.get();

        if (action.getLevel() == ActionLevel.RED
                && (request == null || request.reason() == null || request.reason().isBlank())) {
            log.warn("[HEALTH_MONITOR] RED approval missing reason - actionId={}", id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String actor = currentUsername();
        int updated = actionRepository.tryApprove(
                id, ActionStatus.PENDING, ActionStatus.APPROVED, actor, LocalDateTime.now());
        if (updated == 0) {
            log.warn("[HEALTH_MONITOR] Approve race: already processed - actionId={}", id);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        action = actionRepository.findById(id).orElseThrow();

        // Audit the approval BEFORE executor lookup so a missing executor does
        // not silently drop the APPROVE entry from the audit trail.
        logAudit(AuditAction.APPROVE, action, null,
                request == null ? null : request.reason(), true, null);

        ActionExecutor executor = actionExecutorProvider.getIfAvailable();
        if (executor == null) {
            action.setStatus(ActionStatus.FAILED);
            action.setResultJson(toResultJson(false, "Executor indisponible"));
            actionRepository.save(action);
            metrics.recordActionExecuted(action.getActionType(), false);
            logAudit(AuditAction.EXECUTE, action, null, "Executor indisponible",
                    false, "Executor indisponible");
            return ResponseEntity.ok(mapper.toResponse(action));
        }

        ActionExecutionResult result = executor.execute(action);
        action.setStatus(result.success() ? ActionStatus.EXECUTED : ActionStatus.FAILED);
        action.setExecutedAt(LocalDateTime.now());
        action.setResultJson(toResultJson(result.success(), result.message()));
        HealthProposedAction saved = actionRepository.save(action);
        metrics.recordActionExecuted(saved.getActionType(), result.success());
        logAudit(AuditAction.EXECUTE, saved, null, result.message(),
                result.success(), result.success() ? null : result.message());

        ProposedActionResponse response = mapper.toResponse(saved);
        eventPublisher.publishActionUpdated(response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/actions/{id}/reject")
    @Transactional
    public ResponseEntity<ProposedActionResponse> reject(
            @PathVariable Long id,
            @RequestBody @Valid ActionDecisionRequest request) {

        if (request == null || request.reason() == null || request.reason().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Optional<HealthProposedAction> maybe = actionRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        int updated = actionRepository.tryReject(id, ActionStatus.PENDING, ActionStatus.REJECTED,
                currentUsername(), LocalDateTime.now(), request.reason());
        if (updated == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        HealthProposedAction saved = actionRepository.findById(id).orElseThrow();
        metrics.recordActionRejected(saved.getActionType());
        logAudit(AuditAction.REJECT, saved, null, request.reason(), true, null);
        ProposedActionResponse response = mapper.toResponse(saved);
        eventPublisher.publishActionUpdated(response);
        return ResponseEntity.ok(response);
    }

    // ========== SSE ==========

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return eventPublisher.register();
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private void logAudit(AuditAction auditAction, HealthProposedAction action,
                          Map<String, Object> oldValues, String reason,
                          boolean success, String errorMessage) {
        try {
            Map<String, Object> newValues = reason == null
                    ? Collections.emptyMap()
                    : Map.of("reason", reason);
            auditService.createAuditLog(
                    AuditContextHolder.getContext(),
                    auditAction,
                    AuditEntityType.HEALTH_PROPOSED_ACTION.getName(),
                    action.getId(),
                    action.getActionType().name(),
                    oldValues,
                    newValues,
                    success,
                    errorMessage);
        } catch (Exception e) {
            log.warn("[HEALTH_MONITOR] Audit log failed - actionId={}, error={}",
                    action.getId(), e.getMessage());
        }
    }

    private String toResultJson(boolean success, String message) {
        try {
            return RESULT_MAPPER.writeValueAsString(Map.of(
                    "success", success,
                    "message", message == null ? "" : message));
        } catch (JsonProcessingException e) {
            log.warn("[HEALTH_MONITOR] Failed to serialize result JSON - error={}", e.getMessage());
            return "{\"success\":" + success + ",\"message\":\"\"}";
        }
    }
}
