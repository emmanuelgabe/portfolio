package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.ContactHistoryResponse;
import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.dto.ContactStatsResponse;
import com.emmanuelgabe.portfolio.entity.Contact;
import com.emmanuelgabe.portfolio.mapper.ContactMapper;
import com.emmanuelgabe.portfolio.messaging.event.AutoReplyEvent;
import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.moderation.ContactCategory;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.moderation.ContactModerationService;
import com.emmanuelgabe.portfolio.moderation.ModerationResult;
import com.emmanuelgabe.portfolio.repository.ContactRepository;
import com.emmanuelgabe.portfolio.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ContactService.
 * Handles contact form submissions by publishing events for async email processing,
 * persists contacts with moderation results, and provides admin statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final EventPublisher eventPublisher;
    private final ContactModerationService moderationService;
    private final ContactRepository contactRepository;
    private final ContactMapper contactMapper;
    private final BusinessMetrics metrics;

    @Value("${spring.mail.username}")
    private String recipientEmail;

    @Value("${ai.autoreply.enabled:false}")
    private boolean autoReplyEnabled;

    @Override
    public ContactResponse sendContactEmail(ContactRequest request) {
        log.info("[CONTACT] Processing contact form - from={}, subject={}",
                request.getEmail(), request.getSubject());

        ModerationResult moderation = moderationService.moderate(request);
        persistContact(request, moderation);

        if (!moderation.isApproved()) {
            log.info("[CONTACT] Message rejected by moderation - email={}, reason={}, confidence={}",
                    request.getEmail(), moderation.reason(), moderation.confidenceScore());
            return ContactResponse.success("Message sent successfully");
        }

        String htmlContent = buildEmailContent(request);

        ContactEmailEvent event = ContactEmailEvent.of(
                recipientEmail,
                request.getName(),
                request.getEmail(),
                request.getSubject(),
                request.getMessage(),
                htmlContent
        );

        eventPublisher.publishEmailEvent(event);

        log.info("[CONTACT] Contact email queued - eventId={}, from={}",
                event.getEventId(), request.getEmail());

        publishAutoReplyEvent(request);

        return ContactResponse.success("Message sent successfully");
    }

    @Override
    public ContactStatsResponse getContactStats(LocalDateTime since) {
        log.debug("[CONTACT_ADMIN] Fetching contact stats - since={}", since);

        long total = contactRepository.countSince(since);
        Map<String, Long> categoryCounts = toMap(contactRepository.countByCategorySince(since));
        Map<String, Long> verdictCounts = toMap(contactRepository.countByVerdictSince(since));
        Map<String, Long> dailyActivity = toMap(contactRepository.countDailySince(since));

        return new ContactStatsResponse(total, categoryCounts, verdictCounts, dailyActivity);
    }

    @Override
    public Page<ContactHistoryResponse> getContactHistory(ContactCategory category, Pageable pageable) {
        log.debug("[CONTACT_ADMIN] Fetching contact history - category={}, page={}",
                category, pageable.getPageNumber());

        Page<Contact> contacts = (category != null)
                ? contactRepository.findByCategory(category, pageable)
                : contactRepository.findAll(pageable);

        return contacts.map(contactMapper::toHistoryResponse);
    }

    private void persistContact(ContactRequest request, ModerationResult moderation) {
        try {
            Contact contact = new Contact();
            contact.setName(request.getName());
            contact.setSubject(request.getSubject());
            contact.setMessage(request.getMessage());
            contact.setCategory(moderation.category());
            contact.setModerationVerdict(moderation.verdict());
            contact.setModerationReason(moderation.reason());
            contact.setConfidenceScore(moderation.confidenceScore());
            contactRepository.save(contact);
            log.debug("[CONTACT] Contact persisted - name={}, category={}, verdict={}",
                    request.getName(), moderation.category(), moderation.verdict());
        } catch (Exception e) {
            log.error("[CONTACT] Failed to persist contact - name={}, error={}",
                    request.getName(), e.getMessage());
            metrics.recordContactPersistenceFailure();
        }
    }

    private void publishAutoReplyEvent(ContactRequest request) {
        if (!autoReplyEnabled) {
            return;
        }
        try {
            AutoReplyEvent autoReplyEvent = AutoReplyEvent.of(
                    request.getName(),
                    request.getEmail(),
                    request.getSubject(),
                    request.getMessage(),
                    recipientEmail
            );
            eventPublisher.publishAutoReplyEvent(autoReplyEvent);
            log.info("[CONTACT] Auto-reply event queued - eventId={}, sender={}",
                    autoReplyEvent.getEventId(), request.getEmail());
        } catch (Exception e) {
            log.error("[CONTACT] Failed to publish auto-reply event - email={}, error={}",
                    request.getEmail(), e.getMessage());
        }
    }

    private String buildEmailContent(ContactRequest request) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #f4f4f4; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
                        .content { padding: 20px; background-color: #fff; border: 1px solid #ddd; border-radius: 5px; }
                        .field { margin-bottom: 15px; }
                        .label { font-weight: bold; color: #555; }
                        .value { margin-top: 5px; padding: 10px; background-color: #f9f9f9; border-left: 3px solid #007bff; }
                        .footer { margin-top: 20px; font-size: 12px; color: #777; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2 style="margin: 0; color: #007bff;">New Contact Form Submission</h2>
                        </div>
                        <div class="content">
                            <div class="field">
                                <div class="label">From:</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="field">
                                <div class="label">Email:</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="field">
                                <div class="label">Subject:</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="field">
                                <div class="label">Message:</div>
                                <div class="value">%s</div>
                            </div>
                        </div>
                        <div class="footer">
                            <p>This email was sent from your portfolio contact form.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(request.getName()),
                escapeHtml(request.getEmail()),
                escapeHtml(request.getSubject()),
                escapeHtml(request.getMessage()).replace("\n", "<br>")
        );
    }

    /**
     * Escape HTML special characters to prevent XSS in emails.
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "UNCATEGORIZED";
            long value = ((Number) row[1]).longValue();
            map.put(key, value);
        }
        return map;
    }
}
