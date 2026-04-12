package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.dto.health.HealthReportResponse;
import com.emmanuelgabe.portfolio.dto.health.ProposedActionResponse;
import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;
import com.emmanuelgabe.portfolio.healthmonitor.action.ReportSeverity;
import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import com.emmanuelgabe.portfolio.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled driver for the AI Health Monitor agent.
 *
 * <p>Runs at a configurable cron (default every 15 minutes). Each tick first
 * invokes {@link HealthPreCheckService} -- a fast, non-LLM gate -- and only
 * spends the Gemini budget on a full {@link HealthAgentService#analyze} cycle
 * if an anomaly is detected. When the system is healthy, a lightweight
 * {@code PRECHECK_ONLY} report is persisted so the admin UI still shows a
 * heartbeat.</p>
 *
 * <p>A {@link AtomicBoolean} guards against overlapping runs if a previous
 * cycle is still executing when the next cron tick fires.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.health-monitor.enabled", havingValue = "true")
public class HealthMonitorScheduler {

    private final HealthPreCheckService preCheckService;
    private final HealthAgentService agentService;
    private final HealthReportRepository reportRepository;
    private final HealthProposedActionRepository actionRepository;
    private final HealthMonitorMapper mapper;
    private final HealthMonitorEventPublisher eventPublisher;
    private final HealthMonitorMetrics metrics;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${ai.health-monitor.schedule-cron:0 */15 * * * *}")
    public void runScheduledCycle() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[HEALTH_MONITOR] Previous cycle still running - skipping tick");
            metrics.recordCycleSkippedOverlap();
            return;
        }

        try {
            HealthPreCheckService.PreCheckResult preCheck = preCheckService.run();
            if (!preCheck.anomalyDetected()) {
                log.debug("[HEALTH_MONITOR] Pre-check clean - skipping LLM cycle");
                metrics.recordCycleSkippedPrecheck();
                persistPreCheckOnlyReport();
                return;
            }

            log.info("[HEALTH_MONITOR] Pre-check flagged anomaly - reason={}", preCheck.reason());
            HealthReport report = agentService.analyze(HealthCycleType.SCHEDULED);
            broadcast(report);
        } catch (Exception e) {
            log.error("[HEALTH_MONITOR] Scheduled cycle failed - error={}", e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }

    private void persistPreCheckOnlyReport() {
        HealthReport report = new HealthReport();
        report.setCycleType(HealthCycleType.PRECHECK_ONLY);
        report.setSeverity(ReportSeverity.OK);
        report.setSummary("Pre-check healthy: no LLM cycle triggered");
        report.setAnomaliesJson("[]");
        report.setToolCallsJson("[]");
        report.setDurationMs(0L);
        report.setCreatedAt(LocalDateTime.now());
        HealthReport saved = reportRepository.save(report);
        eventPublisher.publishNewReport(mapper.toResponse(saved));
    }

    private void broadcast(HealthReport report) {
        HealthReportResponse reportResponse = mapper.toResponse(report);
        eventPublisher.publishNewReport(reportResponse);

        List<HealthProposedAction> proposals = actionRepository
                .findByReportIdOrderByCreatedAtAsc(report.getId());
        for (HealthProposedAction action : proposals) {
            if (action.getStatus() == ActionStatus.PENDING) {
                ProposedActionResponse actionResponse = mapper.toResponse(action);
                eventPublisher.publishNewPendingAction(actionResponse);
            }
        }
    }
}
