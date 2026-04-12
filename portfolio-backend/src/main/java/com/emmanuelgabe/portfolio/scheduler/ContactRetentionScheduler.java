package com.emmanuelgabe.portfolio.scheduler;

import com.emmanuelgabe.portfolio.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * GDPR retention scheduler for contact form submissions.
 * Automatically purges contacts older than the configured retention period.
 *
 * <p>Runs daily at 3:30 AM by default. The retention period defaults to 12 months
 * and is configurable via {@code contact.retention.months}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContactRetentionScheduler {

    private final ContactRepository contactRepository;

    @Value("${contact.retention.months:12}")
    private int retentionMonths;

    /**
     * Purges contact records older than the retention period.
     * Runs daily at 3:30 AM (configurable via {@code contact.retention.cron}).
     */
    @Scheduled(cron = "${contact.retention.cron:0 30 3 * * ?}")
    @Transactional
    public void purgeExpiredContacts() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(retentionMonths);
        log.info("[CONTACT_RETENTION] Starting GDPR cleanup - cutoff={}, retentionMonths={}",
                cutoff, retentionMonths);

        try {
            int deleted = contactRepository.deleteExpiredBefore(cutoff);

            log.info("[CONTACT_RETENTION] GDPR cleanup complete - deleted={}", deleted);
        } catch (Exception e) {
            log.error("[CONTACT_RETENTION] GDPR cleanup failed - error={}", e.getMessage(), e);
        }
    }
}
