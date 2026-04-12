package com.emmanuelgabe.portfolio.moderation;

/**
 * Immutable result of an AI moderation analysis on a contact message.
 *
 * @param verdict         the moderation decision (APPROVED or REJECTED)
 * @param category        the message category assigned by AI (nullable if AI unavailable)
 * @param reason          human-readable explanation of the decision
 * @param confidenceScore confidence level between 0.0 and 1.0
 */
public record ModerationResult(
        ModerationVerdict verdict,
        ContactCategory category,
        String reason,
        double confidenceScore
) {

    /**
     * Creates an approved result with maximum confidence and no category.
     * Used as the fail-open default when AI is unavailable.
     *
     * @return an approved moderation result
     */
    public static ModerationResult approved() {
        return new ModerationResult(ModerationVerdict.APPROVED, null, "Approved", 1.0);
    }

    /**
     * Creates a rejected result with the given reason and confidence score.
     * Category is null when the rejection source cannot categorize (e.g. timeout fallback).
     *
     * @param reason          explanation of why the message was rejected
     * @param confidenceScore confidence level between 0.0 and 1.0
     * @return a rejected moderation result
     */
    public static ModerationResult rejected(String reason, double confidenceScore) {
        return new ModerationResult(ModerationVerdict.REJECTED, null, reason, confidenceScore);
    }

    /**
     * Checks if the message was approved.
     *
     * @return true if the verdict is APPROVED
     */
    public boolean isApproved() {
        return verdict == ModerationVerdict.APPROVED;
    }
}
