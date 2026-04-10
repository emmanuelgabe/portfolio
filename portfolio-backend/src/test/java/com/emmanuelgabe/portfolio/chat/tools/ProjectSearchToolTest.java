package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
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

    @Mock
    private ToolCallTracker tracker;

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

    // ========== listProjects ==========

    @Test
    void should_returnAllProjects_when_listProjectsCalled() {
        // Arrange
        ProjectResponse first = new ProjectResponse();
        first.setId(1L);
        first.setTitle("Portfolio Backend");
        ProjectResponse second = new ProjectResponse();
        second.setId(2L);
        second.setTitle("Portfolio Frontend");
        when(projectService.getAllProjects()).thenReturn(List.of(first, second));

        // Act
        List<ProjectResponse> results = projectSearchTool.listProjects();

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ProjectResponse::getTitle)
                .containsExactly("Portfolio Backend", "Portfolio Frontend");
        verify(projectService).getAllProjects();
        verify(tracker).record("listProjects");
    }

    @Test
    void should_returnEmptyList_when_listProjectsHasNoData() {
        // Arrange
        when(projectService.getAllProjects()).thenReturn(List.of());

        // Act
        List<ProjectResponse> results = projectSearchTool.listProjects();

        // Assert
        assertThat(results).isEmpty();
        verify(tracker).record("listProjects");
    }

    // ========== getFeaturedProjects ==========

    @Test
    void should_returnFeaturedProjects_when_getFeaturedProjectsCalled() {
        // Arrange
        ProjectResponse featured = new ProjectResponse();
        featured.setId(10L);
        featured.setTitle("Flagship Project");
        featured.setFeatured(true);
        when(projectService.getFeaturedProjects()).thenReturn(List.of(featured));

        // Act
        List<ProjectResponse> results = projectSearchTool.getFeaturedProjects();

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Flagship Project");
        verify(projectService).getFeaturedProjects();
        verify(tracker).record("getFeaturedProjects");
    }

    @Test
    void should_returnEmptyList_when_getFeaturedProjectsHasNoData() {
        // Arrange
        when(projectService.getFeaturedProjects()).thenReturn(List.of());

        // Act
        List<ProjectResponse> results = projectSearchTool.getFeaturedProjects();

        // Assert
        assertThat(results).isEmpty();
        verify(tracker).record("getFeaturedProjects");
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
