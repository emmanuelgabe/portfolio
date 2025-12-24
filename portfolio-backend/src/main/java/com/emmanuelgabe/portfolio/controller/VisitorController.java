package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.service.VisitorTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for visitor tracking endpoints.
 * Public endpoints for anonymous visitors to send heartbeats.
 */
@Slf4j
@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorTrackingService visitorTrackingService;

    /**
     * Register a heartbeat for a visitor session.
     * Called periodically by the frontend to track active visitors.
     *
     * @param sessionId the unique session identifier from the frontend
     * @return 200 OK on success
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @RequestHeader(value = "X-Session-Id", required = true) String sessionId) {
        log.debug("[VISITOR_HEARTBEAT] Received heartbeat - sessionId={}", sessionId);
        visitorTrackingService.registerHeartbeat(sessionId);
        return ResponseEntity.ok().build();
    }
}
