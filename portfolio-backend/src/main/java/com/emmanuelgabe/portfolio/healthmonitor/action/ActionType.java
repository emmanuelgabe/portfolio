package com.emmanuelgabe.portfolio.healthmonitor.action;

/**
 * Concrete action types the agent can propose.
 * Each type maps to an {@code ActionExecutor} branch and a required {@link ActionLevel}.
 */
public enum ActionType {

    /** Flush Redis cache (full or by pattern). Params: {@code pattern?: string}. */
    FLUSH_REDIS_CACHE(ActionLevel.ORANGE),

    /** Retry messages from the RabbitMQ dead letter queue. Params: {@code count: int}. */
    RETRY_DLQ_MESSAGES(ActionLevel.ORANGE),

    /** Trigger a full Elasticsearch reindex batch job. Params: none. */
    REINDEX_ELASTICSEARCH(ActionLevel.ORANGE),

    /** Trigger the image reprocessing batch job. Params: none. */
    TRIGGER_IMAGE_REPROCESSING(ActionLevel.ORANGE),

    /** Restart a Docker container via the socket proxy. Params: {@code serviceName: string}. */
    RESTART_SERVICE(ActionLevel.RED);

    private final ActionLevel level;

    ActionType(ActionLevel level) {
        this.level = level;
    }

    public ActionLevel getLevel() {
        return level;
    }
}
