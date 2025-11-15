package com.emmanuelgabe.portfolio.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TagEntityTest {

    private Validator validator;
    private Tag tag;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        tag = new Tag();
        tag.setName("Java");
        tag.setColor("#007396");
    }

    @Test
    void whenValidTag_thenNoConstraintViolations() {
        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenNameIsBlank_thenConstraintViolation() {
        // Given
        tag.setName("");

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Tag name is required"));
    }

    @Test
    void whenNameIsNull_thenConstraintViolation() {
        // Given
        tag.setName(null);

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Tag name is required");
    }

    @Test
    void whenNameIsTooShort_thenConstraintViolation() {
        // Given
        tag.setName("A"); // Less than 2 characters

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Tag name must be between 2 and 50 characters");
    }

    @Test
    void whenNameIsTooLong_thenConstraintViolation() {
        // Given
        tag.setName("A".repeat(51)); // More than 50 characters

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Tag name must be between 2 and 50 characters");
    }

    @Test
    void whenColorIsBlank_thenConstraintViolation() {
        // Given
        tag.setColor("");

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Color is required"));
    }

    @Test
    void whenColorIsNull_thenConstraintViolation() {
        // Given
        tag.setColor(null);

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Color is required");
    }

    @Test
    void whenColorIsInvalidFormat_thenConstraintViolation() {
        // Given
        tag.setColor("blue"); // Invalid hex color format

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Color must be a valid hex color code");
    }

    @Test
    void whenColorIsInvalidHex_thenConstraintViolation() {
        // Given
        tag.setColor("#ZZZZZZ"); // Invalid hex characters

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Color must be a valid hex color code");
    }

    @Test
    void whenColorIsMissingHash_thenConstraintViolation() {
        // Given
        tag.setColor("FF5733"); // Missing # prefix

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Color must be a valid hex color code");
    }

    @Test
    void whenColorIsValidShortFormat_thenNoConstraintViolations() {
        // Given
        tag.setColor("#FFF"); // Valid 3-character hex

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenColorIsValidLongFormat_thenNoConstraintViolations() {
        // Given
        tag.setColor("#FFFFFF"); // Valid 6-character hex

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenColorWithLowerCase_thenNoConstraintViolations() {
        // Given
        tag.setColor("#ff5733"); // Valid lowercase hex

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenColorWithMixedCase_thenNoConstraintViolations() {
        // Given
        tag.setColor("#Ff5733"); // Valid mixed case hex

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenNewTag_thenProjectsIsEmptySet() {
        // Given
        Tag newTag = new Tag();

        // Then
        assertThat(newTag.getProjects()).isNotNull();
        assertThat(newTag.getProjects()).isEmpty();
    }

    @Test
    void testMultipleConstraintViolations() {
        // Given
        tag.setName(""); // Blank - triggers both @NotBlank and @Size
        tag.setColor("invalid"); // Invalid format - triggers both @NotBlank and @Pattern

        // When
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void testCommonHexColors() {
        // Test common hex color patterns
        String[] validColors = {
            "#000000", // Black
            "#FFFFFF", // White
            "#FF0000", // Red
            "#00FF00", // Green
            "#0000FF", // Blue
            "#FFF",    // Short white
            "#000",    // Short black
            "#ABC",    // Short mixed
            "#123456", // Numbers
            "#aAbBcC"  // Mixed case
        };

        for (String color : validColors) {
            // Given
            tag.setColor(color);

            // When
            Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Test
    void testInvalidHexColors() {
        // Test invalid hex color patterns
        String[] invalidColors = {
            "000000",     // Missing #
            "#00000",     // 5 characters
            "#0000000",   // 7 characters (too long)
            "#GGG",       // Invalid characters
            "#GGGGGG",    // Invalid characters
            "rgb(0,0,0)", // Wrong format
            "blue",       // Color name
            "#",          // Just hash
            ""            // Empty
        };

        for (String color : invalidColors) {
            // Given
            tag.setColor(color);

            // When
            Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

            // Then
            assertThat(violations).isNotEmpty();
        }
    }
}
