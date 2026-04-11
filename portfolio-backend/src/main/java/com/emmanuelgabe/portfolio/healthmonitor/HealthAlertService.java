package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.emmanuelgabe.portfolio.healthmonitor.action.ReportSeverity;
import com.emmanuelgabe.portfolio.messaging.event.HealthAlertEmailEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Publishes an email alert to the existing email queue when the diagnostic
 * agent produces a CRITICAL report.
 *
 * <p>A simple in-memory cooldown prevents the admin mailbox from being
 * flooded when the system stays in a bad state across several consecutive
 * cycles. The cooldown resets on application restart, which is acceptable:
 * a fresh alert right after a restart is typically desirable.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.health-monitor.enabled", havingValue = "true")
public class HealthAlertService {

    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(30);

    private final EventPublisher eventPublisher;
    private final String alertRecipientEmail;
    private final AtomicReference<Instant> lastAlertAt = new AtomicReference<>();

    public HealthAlertService(EventPublisher eventPublisher,
                              @Value("${spring.mail.username:}") String alertRecipientEmail) {
        this.eventPublisher = eventPublisher;
        this.alertRecipientEmail = alertRecipientEmail;
    }

    public void maybeAlert(HealthReport report) {
        if (report.getSeverity() != ReportSeverity.CRITICAL) {
            return;
        }
        if (alertRecipientEmail == null || alertRecipientEmail.isBlank()) {
            log.debug("[HEALTH_ALERT] Critical report but no recipient configured - reportId={}",
                    report.getId());
            return;
        }
        if (eventPublisher == null || !eventPublisher.isEnabled()) {
            log.debug("[HEALTH_ALERT] Critical report but publisher unavailable - reportId={}",
                    report.getId());
            return;
        }
        Instant now = Instant.now();
        Instant previous = lastAlertAt.get();
        if (previous != null && Duration.between(previous, now).compareTo(ALERT_COOLDOWN) < 0) {
            log.info("[HEALTH_ALERT] Suppressed by cooldown - reportId={}, cooldownMinutes={}",
                    report.getId(), ALERT_COOLDOWN.toMinutes());
            return;
        }
        if (!lastAlertAt.compareAndSet(previous, now)) {
            return;
        }

        HealthAlertEmailEvent event = HealthAlertEmailEvent.builder()
                .recipientEmail(alertRecipientEmail)
                .subject("[Portfolio Health] Anomalie critique detectee - rapport #" + report.getId())
                .body(buildBody(report))
                .reportId(report.getId())
                .severity(report.getSeverity().name())
                .build();
        try {
            eventPublisher.publishEmailEvent(event);
            log.info("[HEALTH_ALERT] Published - reportId={}, recipient={}",
                    report.getId(), alertRecipientEmail);
        } catch (Exception e) {
            log.error("[HEALTH_ALERT] Publish failed - reportId={}, error={}",
                    report.getId(), e.getMessage(), e);
        }
    }

    private String buildBody(HealthReport report) {
        String summary = report.getSummary() == null ? "(aucun resume)" : report.getSummary();
        return "<h2>Anomalie critique detectee</h2>"
                + "<p><strong>Rapport #" + report.getId() + "</strong> ("
                + report.getSeverity() + ")</p>"
                + "<p>" + escapeHtml(summary) + "</p>"
                + "<p>Consultez le tableau de bord admin pour les actions proposees.</p>";
    }

    private static String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
