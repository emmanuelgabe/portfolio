package com.emmanuelgabe.portfolio.healthmonitor.action;

import java.util.List;

/**
 * Structured output target for the diagnostic agent LLM call.
 * Spring AI deserializes the model response directly into this record via
 * {@code chatClient.prompt()...call().entity(HealthReportDraft.class)}.
 *
 * @param severity  overall health severity
 * @param summary   2-3 sentence synthesis in French, plain text
 * @param anomalies concrete findings with evidence (may be empty)
 * @param proposals recommended corrective actions (may be empty)
 */
public record HealthReportDraft(
        ReportSeverity severity,
        String summary,
        List<String> anomalies,
        List<ProposedActionDraft> proposals
) { }
