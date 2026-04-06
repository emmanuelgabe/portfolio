package com.emmanuelgabe.portfolio.moderation;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * No-operation moderation service used when AI moderation is unavailable.
 * Activates automatically when {@link AiContactModerationService} is not created
 * (AI disabled, API key missing, or moderation explicitly disabled).
 * Automatically approves all messages without analysis.
 */
@Slf4j
public class NoOpContactModerationService implements ContactModerationService {

    @Override
    public ModerationResult moderate(ContactRequest request) {
        log.debug("[MODERATION] AI moderation disabled - auto-approving message - email={}",
                request.getEmail());
        return ModerationResult.approved();
    }
}
