package com.emmanuelgabe.portfolio.healthmonitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration for the AI Health Monitor agent.
 * Bound from the {@code ai.health-monitor.*} namespace.
 */
@ConfigurationProperties(prefix = "ai.health-monitor")
public record HealthMonitorProperties(
        boolean enabled,
        boolean demoMode,
        String scheduleCron,
        boolean onDemandBypassPrecheck,
        int actionExpirationMinutes,
        int maxMutationsPerHour,
        Docker docker,
        History history,
        Logs logs
) {

    public record Docker(String host) { }

    public record History(int reportsInContext, int rejectedActionsInContext) { }

    public record Logs(int maxLinesPerCall) { }
}
