package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperienceToolTest {

    @Mock
    private ExperienceService experienceService;

    @Mock
    private ToolCallTracker tracker;

    @InjectMocks
    private ExperienceTool experienceTool;

    // ========== listExperiences ==========

    @Test
    void should_returnAllExperiences_when_listExperiencesCalledWithNullType() {
        // Arrange
        ExperienceResponse exp = new ExperienceResponse();
        exp.setCompany("Acme Corp");
        when(experienceService.getAllExperiences()).thenReturn(List.of(exp));

        // Act
        List<ExperienceResponse> results = experienceTool.listExperiences(null);

        // Assert
        assertThat(results).hasSize(1);
        verify(experienceService).getAllExperiences();
    }

    @Test
    void should_returnAllExperiences_when_listExperiencesCalledWithBlankType() {
        // Arrange
        when(experienceService.getAllExperiences()).thenReturn(List.of());

        // Act
        List<ExperienceResponse> results = experienceTool.listExperiences("  ");

        // Assert
        assertThat(results).isEmpty();
        verify(experienceService).getAllExperiences();
    }

    @Test
    void should_returnFilteredExperiences_when_listExperiencesCalledWithType() {
        // Arrange
        ExperienceResponse exp = new ExperienceResponse();
        exp.setCompany("Tech Corp");
        when(experienceService.getExperiencesByType(ExperienceType.WORK))
                .thenReturn(List.of(exp));

        // Act
        List<ExperienceResponse> results = experienceTool.listExperiences("WORK");

        // Assert
        assertThat(results).hasSize(1);
        verify(experienceService).getExperiencesByType(ExperienceType.WORK);
    }

    @Test
    void should_handleCaseInsensitiveType_when_listExperiencesCalled() {
        // Arrange
        when(experienceService.getExperiencesByType(ExperienceType.EDUCATION))
                .thenReturn(List.of());

        // Act
        experienceTool.listExperiences("education");

        // Assert
        verify(experienceService).getExperiencesByType(ExperienceType.EDUCATION);
    }

    @Test
    void should_throwException_when_listExperiencesCalledWithInvalidType() {
        // Act & Assert
        assertThatThrownBy(() -> experienceTool.listExperiences("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== getRecentExperiences ==========

    @Test
    void should_returnRecentExperiences_when_getRecentExperiencesCalled() {
        // Arrange
        ExperienceResponse exp = new ExperienceResponse();
        exp.setCompany("Recent Corp");
        when(experienceService.getRecentExperiences(3)).thenReturn(List.of(exp));

        // Act
        List<ExperienceResponse> results = experienceTool.getRecentExperiences(3);

        // Assert
        assertThat(results).hasSize(1);
        verify(experienceService).getRecentExperiences(3);
    }

    @Test
    void should_capLimitAt10_when_getRecentExperiencesCalledWithExcessiveLimit() {
        // Arrange
        when(experienceService.getRecentExperiences(10)).thenReturn(List.of());

        // Act
        experienceTool.getRecentExperiences(50);

        // Assert
        verify(experienceService).getRecentExperiences(10);
    }

    @Test
    void should_enforceMinLimit_when_getRecentExperiencesCalledWithZero() {
        // Arrange
        when(experienceService.getRecentExperiences(1)).thenReturn(List.of());

        // Act
        experienceTool.getRecentExperiences(0);

        // Assert
        verify(experienceService).getRecentExperiences(1);
    }
}
