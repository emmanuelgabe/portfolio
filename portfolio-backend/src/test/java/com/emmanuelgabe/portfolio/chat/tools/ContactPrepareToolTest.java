package com.emmanuelgabe.portfolio.chat.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactPrepareToolTest {

    private final ContactPrepareTool contactPrepareTool = new ContactPrepareTool();

    // ========== prepareContact ==========

    @Test
    void should_returnContactFormData_when_prepareContactCalled() {
        // Act
        ContactPrepareTool.ContactFormData result =
                contactPrepareTool.prepareContact("Mission freelance", "Bonjour, je cherche un dev Java.");

        // Assert
        assertThat(result.subject()).isEqualTo("Mission freelance");
        assertThat(result.message()).isEqualTo("Bonjour, je cherche un dev Java.");
    }

    @Test
    void should_preserveExactContent_when_prepareContactCalledWithSpecialChars() {
        // Act
        ContactPrepareTool.ContactFormData result =
                contactPrepareTool.prepareContact(
                        "Question technique",
                        "Comment gerez-vous l'authentification JWT ?");

        // Assert
        assertThat(result.subject()).isEqualTo("Question technique");
        assertThat(result.message()).contains("JWT");
    }
}
