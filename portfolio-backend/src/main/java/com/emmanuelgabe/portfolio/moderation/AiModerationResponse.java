package com.emmanuelgabe.portfolio.moderation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for deserializing the AI model's structured JSON response.
 * Used as the target class for Spring AI's {@code entity()} method.
 *
 * <p>Must be a mutable class (not a record) because Spring AI's
 * {@code BeanOutputConverter} requires a no-arg constructor and setters
 * for Jackson deserialization.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModerationResponse {

    private String verdict;
    private String category;
    private String reason;
    private double confidenceScore;
}
