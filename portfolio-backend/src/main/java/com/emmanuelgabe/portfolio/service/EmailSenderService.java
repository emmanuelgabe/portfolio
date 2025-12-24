package com.emmanuelgabe.portfolio.service;

/**
 * Service interface for sending emails.
 * Abstracts email sending to allow different implementations
 * and easier testing.
 */
public interface EmailSenderService {

    /**
     * Send an HTML email.
     *
     * @param from    the sender email address (for Reply-To)
     * @param to      the recipient email address
     * @param subject the email subject
     * @param body    the HTML body content
     */
    void sendHtmlEmail(String from, String to, String subject, String body);
}
