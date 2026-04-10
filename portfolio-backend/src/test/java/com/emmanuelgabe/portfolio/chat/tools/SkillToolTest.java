package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.service.SkillService;
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
class SkillToolTest {

    @Mock
    private SkillService skillService;

    @Mock
    private ToolCallTracker tracker;

    @InjectMocks
    private SkillTool skillTool;

    // ========== listSkills ==========

    @Test
    void should_returnAllSkills_when_listSkillsCalledWithNullCategory() {
        // Arrange
        SkillResponse skill = new SkillResponse();
        skill.setName("Java");
        when(skillService.getAllSkills()).thenReturn(List.of(skill));

        // Act
        List<SkillResponse> results = skillTool.listSkills(null);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Java");
        verify(skillService).getAllSkills();
    }

    @Test
    void should_returnAllSkills_when_listSkillsCalledWithBlankCategory() {
        // Arrange
        when(skillService.getAllSkills()).thenReturn(List.of());

        // Act
        skillTool.listSkills("  ");

        // Assert
        verify(skillService).getAllSkills();
    }

    @Test
    void should_returnFilteredSkills_when_listSkillsCalledWithCategory() {
        // Arrange
        SkillResponse skill = new SkillResponse();
        skill.setName("Spring Boot");
        when(skillService.getSkillsByCategory(SkillCategory.BACKEND))
                .thenReturn(List.of(skill));

        // Act
        List<SkillResponse> results = skillTool.listSkills("BACKEND");

        // Assert
        assertThat(results).hasSize(1);
        verify(skillService).getSkillsByCategory(SkillCategory.BACKEND);
    }

    @Test
    void should_handleCaseInsensitiveCategory_when_listSkillsCalled() {
        // Arrange
        when(skillService.getSkillsByCategory(SkillCategory.FRONTEND))
                .thenReturn(List.of());

        // Act
        skillTool.listSkills("frontend");

        // Assert
        verify(skillService).getSkillsByCategory(SkillCategory.FRONTEND);
    }

    @Test
    void should_throwException_when_listSkillsCalledWithInvalidCategory() {
        // Act & Assert
        assertThatThrownBy(() -> skillTool.listSkills("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
