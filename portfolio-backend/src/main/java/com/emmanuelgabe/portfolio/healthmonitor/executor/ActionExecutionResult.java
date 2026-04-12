package com.emmanuelgabe.portfolio.healthmonitor.executor;

/**
 * Outcome of executing a proposed action.
 *
 * @param success true if the action ran to completion without error
 * @param message human-readable summary (e.g. "Reindexed 142 projects")
 */
public record ActionExecutionResult(boolean success, String message) {

    public static ActionExecutionResult ok(String message) {
        return new ActionExecutionResult(true, message);
    }

    public static ActionExecutionResult failure(String message) {
        return new ActionExecutionResult(false, message);
    }
}
