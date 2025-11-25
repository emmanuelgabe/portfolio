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
    void should_haveNoConstraintViolations_when_tagIsValid() {
        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveConstraintViolation_when_nameIsBlank() {
        // Arrange
        tag.setName("");

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Tag name is required"));
    }

    @Test
    void should_haveConstraintViolation_when_nameIsNull() {
        // Arrange
        tag.setName(null);

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Tag name is required");
    }

    @Test
    void should_haveConstraintViolation_when_nameIsTooShort() {
        // Arrange
        tag.setName("A"); // Less than 2 characters

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Tag name must be between 2 and 50 characters");
    }

    @Test
    void should_haveConstraintViolation_when_nameIsTooLong() {
        // Arrange
        tag.setName("A".repeat(51)); // More than 50 characters

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Tag name must be between 2 and 50 characters");
    }

    @Test
    void should_haveConstraintViolation_when_colorIsBlank() {
        // Arrange
        tag.setColor("");

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Color is required"));
    }

    @Test
    void should_haveConstraintViolation_when_colorIsNull() {
        // Arrange
        tag.setColor(null);

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Color is required");
    }

    @Test
    void should_haveConstraintViolation_when_colorIsInvalidFormat() {
        // Arrange
        tag.setColor("blue"); // Invalid hex color format

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Color must be a valid hex color code");
    }

    @Test
    void should_haveConstraintViolation_when_colorIsInvalidHex() {
        // Arrange
        tag.setColor("#ZZZZZZ"); // Invalid hex characters

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Color must be a valid hex color code");
    }

    @Test
    void should_haveConstraintViolation_when_colorIsMissingHash() {
        // Arrange
        tag.setColor("FF5733"); // Missing # prefix

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Color must be a valid hex color code");
    }

    @Test
    void should_haveNoConstraintViolations_when_colorIsValidShortFormat() {
        // Arrange
        tag.setColor("#FFF"); // Valid 3-character hex

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveNoConstraintViolations_when_colorIsValidLongFormat() {
        // Arrange
        tag.setColor("#FFFFFF"); // Valid 6-character hex

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveNoConstraintViolations_when_colorWithLowerCase() {
        // Arrange
        tag.setColor("#ff5733"); // Valid lowercase hex

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveNoConstraintViolations_when_colorWithMixedCase() {
        // Arrange
        tag.setColor("#Ff5733"); // Valid mixed case hex

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveEmptyProjectsSet_when_tagIsNew() {
        // Arrange
        Tag newTag = new Tag();

        // Assert
        assertThat(newTag.getProjects()).isNotNull();
        assertThat(newTag.getProjects()).isEmpty();
    }

    @Test
    void should_haveMultipleConstraintViolations_when_multipleFieldsInvalid() {
        // Arrange
        tag.setName(""); // Blank - triggers both @NotBlank and @Size
        tag.setColor("invalid"); // Invalid format - triggers both @NotBlank and @Pattern

        // Act
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

        // Assert
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void should_haveNoConstraintViolations_when_usingCommonHexColors() {
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
            // Arrange
            tag.setColor(color);

            // Act
            Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    @Test
    void should_haveConstraintViolations_when_usingInvalidHexColors() {
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
            // Arrange
            tag.setColor(color);

            // Act
            Set<ConstraintViolation<Tag>> violations = validator.validate(tag);

            // Assert
            assertThat(violations).isNotEmpty();
        }
    }
}
