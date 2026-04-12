package com.emmanuelgabe.portfolio.autoreply;

import java.util.Optional;

/**
 * Service for generating AI-powered auto-replies to contact messages.
 * Uses RAG (Retrieval-Augmented Generation) to produce contextual responses
 * based on portfolio content.
 *
 * <p>Follows the fail-open contract: any failure results in {@link Optional#empty()},
 * never blocking the contact flow. The caller decides what to do when no reply
 * is generated.</p>
 */
public interface AutoReplyService {

    /**
     * Generate a contextual auto-reply for a contact message using RAG.
     *
     * @param senderName  the contact form submitter's name
     * @param senderEmail the contact form submitter's email
     * @param subject     the contact form subject
     * @param message     the raw message text
     * @return the generated reply text, or empty if generation fails or is unavailable
     */
    Optional<String> generateReply(String senderName, String senderEmail, String subject, String message);
}
