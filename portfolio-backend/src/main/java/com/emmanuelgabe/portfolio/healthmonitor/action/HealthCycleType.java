package com.emmanuelgabe.portfolio.healthmonitor.action;

/**
 * Origin of a health diagnostic cycle.
 */
public enum HealthCycleType {

    /** Triggered by the scheduler on its cron interval. */
    SCHEDULED,

    /** Triggered manually by an admin through the analyze-now endpoint. */
    ON_DEMAND,

    /** Pre-check produced a healthy result, LLM was skipped for cost control. */
    PRECHECK_ONLY
}
