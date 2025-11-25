package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.exception.EmailException;
import com.emmanuelgabe.portfolio.service.ContactService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String recipientEmail;

    @Override
    public ContactResponse sendContactEmail(ContactRequest request) {
        log.info("[CONTACT_EMAIL] Sending email - from={}, subject={}", request.getEmail(), request.getSubject());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(request.getEmail());
            helper.setTo(recipientEmail);
            helper.setSubject("[Portfolio Contact] " + request.getSubject());

            String htmlContent = buildEmailContent(request);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            log.info("[CONTACT_EMAIL] Email sent successfully - from={}, to={}", request.getEmail(), recipientEmail);
            return ContactResponse.success("Message sent successfully");

        } catch (MessagingException e) {
            log.error("[CONTACT_EMAIL] Failed to send email - from={}, to={}, error={}",
                    request.getEmail(), recipientEmail, e.getMessage(), e);
            throw new EmailException("Failed to send email", e);
        }
    }

    private String buildEmailContent(ContactRequest request) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #f4f4f4; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
                        .content { padding: 20px; background-color: #fff; border: 1px solid #ddd; border-radius: 5px; }
                        .field { margin-bottom: 15px; }
                        .label { font-weight: bold; color: #555; }
                        .value { margin-top: 5px; padding: 10px; background-color: #f9f9f9; border-left: 3px solid #007bff; }
                        .footer { margin-top: 20px; font-size: 12px; color: #777; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2 style="margin: 0; color: #007bff;">New Contact Form Submission</h2>
                        </div>
                        <div class="content">
                            <div class="field">
                                <div class="label">From:</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="field">
                                <div class="label">Email:</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="field">
                                <div class="label">Subject:</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="field">
                                <div class="label">Message:</div>
                                <div class="value">%s</div>
                            </div>
                        </div>
                        <div class="footer">
                            <p>This email was sent from your portfolio contact form.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(request.getName()),
                escapeHtml(request.getEmail()),
                escapeHtml(request.getSubject()),
                escapeHtml(request.getMessage()).replace("\n", "<br>")
        );
    }

    /**
     * Escape HTML special characters to prevent XSS in emails
     *
     * @param input String to escape
     * @return Escaped string
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
