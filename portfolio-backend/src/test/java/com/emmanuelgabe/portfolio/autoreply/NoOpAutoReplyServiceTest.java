package com.emmanuelgabe.portfolio.autoreply;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NoOpAutoReplyServiceTest {

    @InjectMocks
    private NoOpAutoReplyService noOpAutoReplyService;

    @Test
    void should_returnEmpty_when_generateReplyCalled() {
        // Act
        Optional<String> result = noOpAutoReplyService.generateReply(
                "John", "john@example.com", "Hello", "Hi there");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_generateReplyCalledWithNullValues() {
        // Act
        Optional<String> result = noOpAutoReplyService.generateReply(null, null, null, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_notThrowException_when_generateReplyCalledWithAnyInput() {
        // Act & Assert
        Optional<String> result = noOpAutoReplyService.generateReply(
                "Jane Doe", "jane@example.com", "Project Inquiry",
                "I would like to discuss a collaboration opportunity");

        assertThat(result).isEmpty();
    }
}
