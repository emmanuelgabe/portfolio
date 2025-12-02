package com.emmanuelgabe.portfolio.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkillEntityTest {

    private Validator validator;
    private Skill skill;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        skill = new Skill();
        skill.setName("Spring Boot");
        skill.setIcon("fa-solid fa-code");
        skill.setIconType(IconType.FONT_AWESOME);
        skill.setColor("#6DB33F");
        skill.setCategory(SkillCategory.BACKEND);
        skill.setDisplayOrder(1);
    }

    @Test
    void should_haveNoConstraintViolations_when_skillIsValid() {
        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveConstraintViolation_when_nameIsBlank() {
        // Arrange
        skill.setName("");

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Skill name is required"));
    }

    @Test
    void should_haveConstraintViolation_when_nameIsNull() {
        // Arrange
        skill.setName(null);

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Skill name is required");
    }

    @Test
    void should_haveConstraintViolation_when_nameIsTooShort() {
        // Arrange
        skill.setName("A"); // Less than 2 characters

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Skill name must be between 2 and 50 characters");
    }

    @Test
    void should_haveConstraintViolation_when_nameIsTooLong() {
        // Arrange
        skill.setName("A".repeat(51)); // More than 50 characters

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Skill name must be between 2 and 50 characters");
    }

    @Test
    void should_haveNoConstraintViolation_when_iconIsBlank() {
        // Arrange - icon is now optional (can be empty for CUSTOM_SVG type)
        skill.setIcon("");
        skill.setIconType(IconType.CUSTOM_SVG);
        skill.setCustomIconUrl("/uploads/icons/test.svg");

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveNoConstraintViolation_when_iconIsNull() {
        // Arrange - icon is now optional (can be null for CUSTOM_SVG type)
        skill.setIcon(null);
        skill.setIconType(IconType.CUSTOM_SVG);
        skill.setCustomIconUrl("/uploads/icons/test.svg");

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveConstraintViolation_when_iconIsTooLong() {
        // Arrange
        skill.setIcon("A".repeat(51)); // More than 50 characters

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Icon class cannot exceed 50 characters");
    }

    @Test
    void should_haveConstraintViolation_when_iconTypeIsNull() {
        // Arrange
        skill.setIconType(null);

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Icon type is required");
    }

    @Test
    void should_haveNoConstraintViolation_when_customIconUrlIsValid() {
        // Arrange
        skill.setIconType(IconType.CUSTOM_SVG);
        skill.setIcon(null);
        skill.setCustomIconUrl("/uploads/icons/angular.svg");

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveConstraintViolation_when_customIconUrlIsTooLong() {
        // Arrange
        skill.setIconType(IconType.CUSTOM_SVG);
        skill.setCustomIconUrl("A".repeat(501)); // More than 500 characters

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Custom icon URL cannot exceed 500 characters");
    }

    @Test
    void should_haveConstraintViolation_when_colorIsBlank() {
        // Arrange
        skill.setColor("");

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Color is required"));
    }

    @Test
    void should_haveConstraintViolation_when_colorIsNull() {
        // Arrange
        skill.setColor(null);

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Color is required");
    }

    @Test
    void should_haveConstraintViolation_when_colorIsInvalidFormat() {
        // Arrange
        skill.setColor("red"); // Invalid hex color format

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Color must be a valid hex color code");
    }

    @Test
    void should_haveConstraintViolation_when_colorIsInvalidHex() {
        // Arrange
        skill.setColor("#GGGGGG"); // Invalid hex characters

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Color must be a valid hex color code");
    }

    @Test
    void should_haveNoConstraintViolations_when_colorIsValidShortFormat() {
        // Arrange
        skill.setColor("#FFF"); // Valid 3-character hex

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveNoConstraintViolations_when_colorIsValidLongFormat() {
        // Arrange
        skill.setColor("#FFFFFF"); // Valid 6-character hex

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveConstraintViolation_when_displayOrderIsBelowMinimum() {
        // Arrange
        skill.setDisplayOrder(-1); // Below 0

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Display order must be at least 0");
    }

    @Test
    void should_haveNoConstraintViolations_when_displayOrderIsZero() {
        // Arrange
        skill.setDisplayOrder(0); // Minimum valid value

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveDisplayOrderDefaultToZero_when_skillIsNew() {
        // Arrange
        Skill newSkill = new Skill();

        // Assert
        assertThat(newSkill.getDisplayOrder()).isEqualTo(0);
    }

    @Test
    void should_haveMultipleConstraintViolations_when_multipleFieldsInvalid() {
        // Arrange
        skill.setName(""); // Blank - triggers both @NotBlank and @Size
        skill.setIconType(null); // Null - required
        skill.setColor("invalid"); // Invalid format - triggers both @NotBlank and @Pattern
        skill.setDisplayOrder(-5); // Below minimum

        // Act
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Assert
        assertThat(violations).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void should_haveNoConstraintViolations_when_usingAllCategories() {
        // Test that all categories are valid
        for (SkillCategory category : SkillCategory.values()) {
            // Arrange
            skill.setCategory(category);

            // Act
            Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    @Test
    void should_returnCorrectDisplayNames_when_gettingCategoryDisplayNames() {
        // Assert
        assertThat(SkillCategory.FRONTEND.getDisplayName()).isEqualTo("Frontend");
        assertThat(SkillCategory.BACKEND.getDisplayName()).isEqualTo("Backend");
        assertThat(SkillCategory.DATABASE.getDisplayName()).isEqualTo("Database");
        assertThat(SkillCategory.DEVOPS.getDisplayName()).isEqualTo("DevOps");
        assertThat(SkillCategory.TOOLS.getDisplayName()).isEqualTo("Tools");
    }
}
