package com.emmanuelgabe.portfolio.moderation;

/**
 * Categories assigned by AI moderation to classify contact form submissions.
 *
 * <ul>
 *   <li>{@code MISSION} - Job offers, freelance requests, collaboration proposals</li>
 *   <li>{@code QUESTION} - Technical questions, requests for information</li>
 *   <li>{@code FEEDBACK} - Compliments, constructive criticism, portfolio comments</li>
 *   <li>{@code SPAM} - SEO spam, marketing, bulk outreach, suspicious links</li>
 *   <li>{@code INAPPROPRIATE} - Abusive language, threats, harassment, hate speech</li>
 * </ul>
 */
public enum ContactCategory {
    MISSION,
    QUESTION,
    FEEDBACK,
    SPAM,
    INAPPROPRIATE
}
