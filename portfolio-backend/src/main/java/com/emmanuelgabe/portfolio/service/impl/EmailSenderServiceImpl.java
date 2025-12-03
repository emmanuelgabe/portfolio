package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.exception.EmailException;
import com.emmanuelgabe.portfolio.service.EmailSenderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implementation of EmailSenderService using Spring JavaMailSender.
 * Handles the actual SMTP communication for sending emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderServiceImpl implements EmailSenderService {

    private final JavaMailSender mailSender;

    @Override
    @Retry(name = "emailService")
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendHtmlEmailFallback")
    public void sendHtmlEmail(String from, String to, String subject, String body) {
        log.debug("[EMAIL_SENDER] Sending email - from={}, to={}, subject={}", from, to, subject);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(mimeMessage);

            log.info("[EMAIL_SENDER] Email sent successfully - from={}, to={}", from, to);

        } catch (MessagingException e) {
            log.error("[EMAIL_SENDER] Failed to send email - from={}, to={}, error={}",
                    from, to, e.getMessage(), e);
            throw new EmailException("Failed to send email", e);
        }
    }

    /**
     * Fallback method called when circuit breaker is open.
     * Logs the failure and re-throws to allow RabbitMQ DLQ handling.
     */
    private void sendHtmlEmailFallback(String from, String to, String subject,
            String body, Throwable t) {
        log.warn("[EMAIL_SENDER] Circuit breaker fallback triggered - to={}, reason={}",
                to, t.getMessage());
        throw new EmailException("Email service unavailable, message queued for retry", t);
    }
}
