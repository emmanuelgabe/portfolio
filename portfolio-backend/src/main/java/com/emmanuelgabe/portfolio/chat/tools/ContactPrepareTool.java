package com.emmanuelgabe.portfolio.chat.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Agent tool for preparing a pre-filled contact form.
 * Does NOT send any message -- returns structured data that the frontend
 * widget uses to open the contact form with pre-filled fields.
 */
@Slf4j
public class ContactPrepareTool {

    /**
     * Prepare contact form data for the visitor.
     *
     * @param subject suggested subject line
     * @param message suggested message body
     * @return pre-filled form data for the frontend widget
     */
    @Tool(description = "Prepare a pre-filled contact form for the visitor to send a message "
            + "to Emmanuel. Returns subject and message that will pre-fill the form. "
            + "This does NOT send any message -- the visitor must review and submit the form.")
    public ContactFormData prepareContact(
            @ToolParam(description = "Subject line for the contact message") String subject,
            @ToolParam(description = "Suggested message body for the contact form") String message) {
        log.debug("[CHAT_TOOL] prepareContact - subject={}", subject);
        return new ContactFormData(subject, message);
    }

    /**
     * Structured data returned to the frontend for contact form pre-fill.
     *
     * @param subject the suggested subject line
     * @param message the suggested message body
     */
    public record ContactFormData(String subject, String message) {
    }
}
