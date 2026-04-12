package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.dto.health.HealthReportResponse;
import com.emmanuelgabe.portfolio.dto.health.ProposedActionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts live health monitor events (new reports, new pending actions,
 * action decisions) to all currently-connected admin SSE clients.
 *
 * <p>Kept out of the controller so both the agent service and the scheduler
 * can publish without a circular dependency.</p>
 */
@Slf4j
@Component
public class HealthMonitorEventPublisher {

    // Clients reconnect transparently on timeout, so a finite value prevents
    // unbounded emitter accumulation when proxies drop silent connections.
    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(e -> emitters.remove(emitter));
        log.debug("[HEALTH_SSE] Client connected - total={}", emitters.size());
        return emitter;
    }

    public void publishNewReport(HealthReportResponse report) {
        broadcast("new_report", report);
    }

    public void publishNewPendingAction(ProposedActionResponse action) {
        broadcast("new_pending_action", action);
    }

    public void publishActionUpdated(ProposedActionResponse action) {
        broadcast("action_updated", action);
    }

    private void broadcast(String eventName, Object payload) {
        if (emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitters.remove(emitter);
                log.debug("[HEALTH_SSE] Send failed, client removed - error={}", e.getMessage());
            }
        }
    }
}
