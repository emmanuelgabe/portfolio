package com.emmanuelgabe.portfolio.healthmonitor.action;

import java.util.Map;

/**
 * Structured output record describing an action proposed by the diagnostic agent.
 * Deserialized directly from the LLM response via Spring AI's {@code .entity()} call.
 *
 * <p>The LLM must set {@link #level()} and {@link #type()} consistently with
 * {@link ActionType#getLevel()}; server-side validation enforces this before persistence.</p>
 *
 * @param level         risk tier (GREEN actions are never persisted)
 * @param type          concrete action type from {@link ActionType}
 * @param params        action-specific parameters (validated per type)
 * @param justification human-readable explanation in French, shown to the admin
 */
public record ProposedActionDraft(
        ActionLevel level,
        ActionType type,
        Map<String, Object> params,
        String justification
) { }
