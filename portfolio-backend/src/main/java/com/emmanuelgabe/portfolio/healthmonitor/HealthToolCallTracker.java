package com.emmanuelgabe.portfolio.healthmonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-cycle tracker that records which diagnostic tools the agent invoked.
 * Stored in the {@code HealthReport.toolCallsJson} field for audit and
 * dashboard display.
 */
public final class HealthToolCallTracker {

    private static final HealthToolCallTracker NOOP = new HealthToolCallTracker(true);

    private final List<String> calls;
    private final boolean noop;

    public HealthToolCallTracker() {
        this(false);
    }

    private HealthToolCallTracker(boolean noop) {
        this.noop = noop;
        this.calls = noop ? Collections.emptyList() : new CopyOnWriteArrayList<>();
    }

    public static HealthToolCallTracker noop() {
        return NOOP;
    }

    public void record(String toolName) {
        if (!noop) {
            calls.add(toolName);
        }
    }

    public List<String> getCalls() {
        return new ArrayList<>(calls);
    }
}
