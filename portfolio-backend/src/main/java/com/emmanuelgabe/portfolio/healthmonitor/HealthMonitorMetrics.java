package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.healthmonitor.action.ActionLevel;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Micrometer metrics for the AI Health Monitor agent. Exposed via
 * Actuator/Prometheus and visualized in a dedicated Grafana dashboard
 * (parallel to the moderation dashboard).
 */
@Component
public class HealthMonitorMetrics {

    private final MeterRegistry registry;
    private final Timer cycleTimer;
    private final Counter cyclesSuccess;
    private final Counter cyclesFailure;
    private final Counter cyclesSkippedPrecheck;
    private final Counter cyclesSkippedOverlap;

    public HealthMonitorMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.cycleTimer = Timer.builder("portfolio.health.cycle.duration")
                .description("Duration of a health diagnostic cycle")
                .register(registry);
        this.cyclesSuccess = Counter.builder("portfolio.health.cycles.total")
                .tag("result", "success")
                .register(registry);
        this.cyclesFailure = Counter.builder("portfolio.health.cycles.total")
                .tag("result", "failure")
                .register(registry);
        this.cyclesSkippedPrecheck = Counter.builder("portfolio.health.cycles.total")
                .tag("result", "skipped_precheck")
                .register(registry);
        this.cyclesSkippedOverlap = Counter.builder("portfolio.health.cycles.total")
                .tag("result", "skipped_overlap")
                .register(registry);
    }

    public void recordCycleDuration(Duration duration) {
        cycleTimer.record(duration);
    }

    public void recordCycleSuccess() {
        cyclesSuccess.increment();
    }

    public void recordCycleFailure() {
        cyclesFailure.increment();
    }

    public void recordCycleSkippedPrecheck() {
        cyclesSkippedPrecheck.increment();
    }

    public void recordCycleSkippedOverlap() {
        cyclesSkippedOverlap.increment();
    }

    public void recordProposedAction(ActionLevel level, ActionType type) {
        Counter.builder("portfolio.health.actions.proposed")
                .tag("level", level.name())
                .tag("type", type.name())
                .register(registry)
                .increment();
    }

    public void recordActionExecuted(ActionType type, boolean success) {
        Counter.builder("portfolio.health.actions.executed")
                .tag("type", type.name())
                .tag("result", success ? "success" : "failure")
                .register(registry)
                .increment();
    }

    public void recordActionRejected(ActionType type) {
        Counter.builder("portfolio.health.actions.rejected")
                .tag("type", type.name())
                .register(registry)
                .increment();
    }

    public void recordActionExpired(ActionType type) {
        Counter.builder("portfolio.health.actions.expired")
                .tag("type", type.name())
                .register(registry)
                .increment();
    }

    public void recordProposalRejectedLevelMismatch() {
        Counter.builder("portfolio.health.proposals.rejected")
                .tag("reason", "level_mismatch")
                .register(registry)
                .increment();
    }
}
