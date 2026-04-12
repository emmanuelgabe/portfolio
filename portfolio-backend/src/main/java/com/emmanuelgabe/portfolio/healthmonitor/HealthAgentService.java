package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;

/**
 * Entry point of the AI health diagnostic agent.
 *
 * <p>Implementations either call the LLM with the green toolset and structured
 * output ({@code AiHealthAgentService}) or return a no-op report when AI is
 * disabled ({@code NoOpHealthAgentService}).</p>
 */
public interface HealthAgentService {

    /**
     * Run one diagnostic cycle and persist the resulting report and proposed actions.
     *
     * @param cycleType origin of the cycle (scheduled, on-demand, or pre-check only)
     * @return the persisted report (never {@code null}; severity UNKNOWN on failure)
     */
    HealthReport analyze(HealthCycleType cycleType);
}
