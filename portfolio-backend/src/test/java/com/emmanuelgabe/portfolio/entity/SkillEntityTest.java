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
        skill.setIcon("bi-spring");
        skill.setColor("#6DB33F");
        skill.setCategory(SkillCategory.BACKEND);
        skill.setLevel(85);
        skill.setDisplayOrder(1);
    }

    @Test
    void whenValidSkill_thenNoConstraintViolations() {
        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenNameIsBlank_thenConstraintViolation() {
        // Given
        skill.setName("");

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Skill name is required"));
    }

    @Test
    void whenNameIsNull_thenConstraintViolation() {
        // Given
        skill.setName(null);

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Skill name is required");
    }

    @Test
    void whenNameIsTooShort_thenConstraintViolation() {
        // Given
        skill.setName("A"); // Less than 2 characters

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Skill name must be between 2 and 50 characters");
    }

    @Test
    void whenNameIsTooLong_thenConstraintViolation() {
        // Given
        skill.setName("A".repeat(51)); // More than 50 characters

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Skill name must be between 2 and 50 characters");
    }

    @Test
    void whenIconIsBlank_thenConstraintViolation() {
        // Given
        skill.setIcon("");

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Icon is required");
    }

    @Test
    void whenIconIsNull_thenConstraintViolation() {
        // Given
        skill.setIcon(null);

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Icon is required");
    }

    @Test
    void whenIconIsTooLong_thenConstraintViolation() {
        // Given
        skill.setIcon("A".repeat(51)); // More than 50 characters

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Icon class cannot exceed 50 characters");
    }

    @Test
    void whenColorIsBlank_thenConstraintViolation() {
        // Given
        skill.setColor("");

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Color is required"));
    }

    @Test
    void whenColorIsNull_thenConstraintViolation() {
        // Given
        skill.setColor(null);

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Color is required");
    }

    @Test
    void whenColorIsInvalidFormat_thenConstraintViolation() {
        // Given
        skill.setColor("red"); // Invalid hex color format

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Color must be a valid hex color code");
    }

    @Test
    void whenColorIsInvalidHex_thenConstraintViolation() {
        // Given
        skill.setColor("#GGGGGG"); // Invalid hex characters

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Color must be a valid hex color code");
    }

    @Test
    void whenColorIsValidShortFormat_thenNoConstraintViolations() {
        // Given
        skill.setColor("#FFF"); // Valid 3-character hex

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenColorIsValidLongFormat_thenNoConstraintViolations() {
        // Given
        skill.setColor("#FFFFFF"); // Valid 6-character hex

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenLevelIsBelowMinimum_thenConstraintViolation() {
        // Given
        skill.setLevel(-1); // Below 0

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Level must be at least 0");
    }

    @Test
    void whenLevelIsAboveMaximum_thenConstraintViolation() {
        // Given
        skill.setLevel(101); // Above 100

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Level cannot exceed 100");
    }

    @Test
    void whenLevelIsZero_thenNoConstraintViolations() {
        // Given
        skill.setLevel(0); // Minimum valid value

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenLevelIsOneHundred_thenNoConstraintViolations() {
        // Given
        skill.setLevel(100); // Maximum valid value

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenDisplayOrderIsBelowMinimum_thenConstraintViolation() {
        // Given
        skill.setDisplayOrder(-1); // Below 0

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Display order must be at least 0");
    }

    @Test
    void whenDisplayOrderIsZero_thenNoConstraintViolations() {
        // Given
        skill.setDisplayOrder(0); // Minimum valid value

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenNewSkill_thenDisplayOrderDefaultsToZero() {
        // Given
        Skill newSkill = new Skill();

        // Then
        assertThat(newSkill.getDisplayOrder()).isEqualTo(0);
    }

    @Test
    void testMultipleConstraintViolations() {
        // Given
        skill.setName(""); // Blank - triggers both @NotBlank and @Size
        skill.setIcon(null); // Null
        skill.setColor("invalid"); // Invalid format - triggers both @NotBlank and @Pattern
        skill.setLevel(-10); // Below minimum
        skill.setDisplayOrder(-5); // Below minimum

        // When
        Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

        // Then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void testAllCategories() {
        // Test that all categories are valid
        for (SkillCategory category : SkillCategory.values()) {
            // Given
            skill.setCategory(category);

            // When
            Set<ConstraintViolation<Skill>> violations = validator.validate(skill);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Test
    void testCategoryDisplayNames() {
        // Then
        assertThat(SkillCategory.FRONTEND.getDisplayName()).isEqualTo("Frontend");
        assertThat(SkillCategory.BACKEND.getDisplayName()).isEqualTo("Backend");
        assertThat(SkillCategory.DATABASE.getDisplayName()).isEqualTo("Database");
        assertThat(SkillCategory.DEVOPS.getDisplayName()).isEqualTo("DevOps");
        assertThat(SkillCategory.TOOLS.getDisplayName()).isEqualTo("Tools");
    }
}
