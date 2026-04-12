package com.emmanuelgabe.portfolio.moderation;

import com.emmanuelgabe.portfolio.dto.ContactRequest;

/**
 * Service responsible for moderating contact form submissions using AI analysis.
 *
 * <p>Implementations must follow the <strong>fail-open</strong> contract:
 * if the AI is unavailable or returns an unexpected response, the message
 * must be approved to avoid blocking legitimate contact attempts.</p>
 */
public interface ContactModerationService {

    /**
     * Analyzes a contact message for spam, toxicity, and professional relevance.
     *
     * @param request the contact form data to analyze
     * @return the moderation result (never null, defaults to APPROVED on failure)
     */
    ModerationResult moderate(ContactRequest request);
}
