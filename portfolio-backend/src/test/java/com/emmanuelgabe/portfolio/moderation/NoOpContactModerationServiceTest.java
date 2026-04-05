package com.emmanuelgabe.portfolio.moderation;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class NoOpContactModerationServiceTest {

    @InjectMocks
    private NoOpContactModerationService noOpContactModerationService;

    // ========== Moderate Tests ==========

    @Test
    void should_returnApproved_when_moderateCalled() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Test Subject",
                "This is a test message"
        );

        // Act
        ModerationResult result = noOpContactModerationService.moderate(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isApproved()).isTrue();
        assertThat(result.verdict()).isEqualTo(ModerationVerdict.APPROVED);
    }

    @Test
    void should_returnConfidenceOne_when_moderateCalled() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "Jane Doe",
                "jane@example.com",
                "Subject",
                "A valid message content"
        );

        // Act
        ModerationResult result = noOpContactModerationService.moderate(request);

        // Assert
        assertThat(result.confidenceScore()).isEqualTo(1.0);
        assertThat(result.reason()).isEqualTo("Approved");
    }

    @Test
    void should_notThrowException_when_moderateCalledWithAnyRequest() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "Spam Bot",
                "spam@bot.com",
                "BUY NOW!!!",
                "Click here for free money"
        );

        // Act & Assert
        assertThatCode(() -> noOpContactModerationService.moderate(request))
                .doesNotThrowAnyException();
    }
}
