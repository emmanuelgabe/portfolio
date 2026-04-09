package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.service.ProjectService;
import com.emmanuelgabe.portfolio.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectSearchToolTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private ProjectSearchTool projectSearchTool;

    // ========== searchProjects ==========

    @Test
    void should_returnResults_when_searchProjectsCalledWithQuery() {
        // Arrange
        ProjectSearchResult result = new ProjectSearchResult();
        result.setId(1L);
        result.setTitle("Portfolio Backend");
        when(searchService.searchProjects("Docker")).thenReturn(List.of(result));

        // Act
        List<ProjectSearchResult> results = projectSearchTool.searchProjects("Docker");

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Portfolio Backend");
        verify(searchService).searchProjects("Docker");
    }

    @Test
    void should_returnEmptyList_when_searchProjectsFindsNoMatch() {
        // Arrange
        when(searchService.searchProjects("nonexistent")).thenReturn(List.of());

        // Act
        List<ProjectSearchResult> results = projectSearchTool.searchProjects("nonexistent");

        // Assert
        assertThat(results).isEmpty();
    }

    // ========== getProjectDetails ==========

    @Test
    void should_returnProject_when_getProjectDetailsCalledWithValidId() {
        // Arrange
        ProjectResponse project = new ProjectResponse();
        project.setId(1L);
        project.setTitle("Portfolio Backend");
        project.setTechStack("Spring Boot, Docker");
        when(projectService.getProjectById(1L)).thenReturn(project);

        // Act
        ProjectResponse result = projectSearchTool.getProjectDetails(1L);

        // Assert
        assertThat(result.getTitle()).isEqualTo("Portfolio Backend");
        assertThat(result.getTechStack()).isEqualTo("Spring Boot, Docker");
        verify(projectService).getProjectById(1L);
    }
}
