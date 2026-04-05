package com.emmanuelgabe.portfolio.autoreply;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * No-operation auto-reply service used when AI auto-reply is unavailable.
 * Activates automatically when {@link AiAutoReplyService} is not created
 * (AI disabled, API key missing, VectorStore unavailable, or auto-reply explicitly disabled).
 * Returns empty to skip reply generation silently.
 */
@Slf4j
public class NoOpAutoReplyService implements AutoReplyService {

    @Override
    public Optional<String> generateReply(String senderName, String senderEmail,
                                          String subject, String message) {
        log.debug("[AUTO_REPLY] Auto-reply disabled - skipping - email={}", senderEmail);
        return Optional.empty();
    }
}
