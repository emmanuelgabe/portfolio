package com.emmanuelgabe.portfolio.healthmonitor.executor;

import com.emmanuelgabe.portfolio.entity.HealthProposedAction;

/**
 * Executes a proposed action once the admin has approved it. Implementations
 * dispatch on {@link HealthProposedAction#getActionType()} and are the only
 * component allowed to perform side effects.
 */
public interface ActionExecutor {

    ActionExecutionResult execute(HealthProposedAction action);
}
