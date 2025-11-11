package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project testProject;
    private Tag testTag;

    @BeforeEach
    void setUp() {
        testTag = new Tag("Java", "#FF5733");
        testTag.setId(1L);

        testProject = new Project();
        testProject.setId(1L);
        testProject.setTitle("Test Project");
        testProject.setDescription("Test Description for project");
        testProject.setTechStack("Java, Spring Boot");
        testProject.setGithubUrl("https://github.com/test/project");
        testProject.setImageUrl("https://example.com/image.jpg");
        testProject.setDemoUrl("https://example.com/demo");
        testProject.setFeatured(true);
        testProject.setCreatedAt(LocalDateTime.now());
        testProject.setUpdatedAt(LocalDateTime.now());
        testProject.setTags(new HashSet<>(Collections.singletonList(testTag)));
    }

    @Test
    void getAllProjects_ShouldReturnListOfProjects() {
        // Arrange
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findAll()).thenReturn(projects);

        // Act
        List<ProjectResponse> result = projectService.getAllProjects();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Project");
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    void getProjectById_WhenProjectExists_ShouldReturnProject() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        // Act
        ProjectResponse result = projectService.getProjectById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Project");
        verify(projectRepository, times(1)).findById(1L);
    }

    @Test
    void getProjectById_WhenProjectNotFound_ShouldThrowException() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
        verify(projectRepository, times(1)).findById(999L);
    }

    @Test
    void createProject_WithoutTags_ShouldReturnCreatedProject() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("New Project");
        request.setDescription("New Description for project");
        request.setTechStack("Java, Spring");
        request.setFeatured(false);

        Project savedProject = new Project();
        savedProject.setId(2L);
        savedProject.setTitle(request.getTitle());
        savedProject.setDescription(request.getDescription());
        savedProject.setTechStack(request.getTechStack());
        savedProject.setFeatured(false);
        savedProject.setTags(new HashSet<>());

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        // Act
        ProjectResponse result = projectService.createProject(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Project");
        assertThat(result.getFeatured()).isFalse();
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void createProject_WithTags_ShouldReturnCreatedProjectWithTags() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("New Project");
        request.setDescription("New Description for project");
        request.setTechStack("Java, Spring");
        request.setTagIds(new HashSet<>(Collections.singletonList(1L)));

        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        ProjectResponse result = projectService.createProject(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTags()).hasSize(1);
        verify(tagRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void createProject_WithInvalidTagId_ShouldThrowException() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("New Project");
        request.setDescription("New Description for project");
        request.setTechStack("Java, Spring");
        request.setTagIds(new HashSet<>(Collections.singletonList(999L)));

        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tag not found");
        verify(tagRepository, times(1)).findById(999L);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void updateProject_WhenProjectExists_ShouldReturnUpdatedProject() {
        // Arrange
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description for project");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        ProjectResponse result = projectService.updateProject(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(projectRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void updateProject_WhenProjectNotFound_ShouldThrowException() {
        // Arrange
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setTitle("Updated Title");

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.updateProject(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
        verify(projectRepository, times(1)).findById(999L);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void updateProject_WithTags_ShouldUpdateTags() {
        // Arrange
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setTagIds(new HashSet<>(Collections.singletonList(1L)));

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        ProjectResponse result = projectService.updateProject(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(tagRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void deleteProject_WhenProjectExists_ShouldDeleteProject() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        doNothing().when(projectRepository).delete(testProject);

        // Act
        projectService.deleteProject(1L);

        // Assert
        verify(projectRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).delete(testProject);
    }

    @Test
    void deleteProject_WhenProjectNotFound_ShouldThrowException() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.deleteProject(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
        verify(projectRepository, times(1)).findById(999L);
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    void getFeaturedProjects_ShouldReturnFeaturedProjects() {
        // Arrange
        List<Project> featuredProjects = Arrays.asList(testProject);
        when(projectRepository.findByFeaturedTrue()).thenReturn(featuredProjects);

        // Act
        List<ProjectResponse> result = projectService.getFeaturedProjects();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFeatured()).isTrue();
        verify(projectRepository, times(1)).findByFeaturedTrue();
    }

    @Test
    void searchByTitle_ShouldReturnMatchingProjects() {
        // Arrange
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findByTitleContainingIgnoreCase("Test")).thenReturn(projects);

        // Act
        List<ProjectResponse> result = projectService.searchByTitle("Test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).contains("Test");
        verify(projectRepository, times(1)).findByTitleContainingIgnoreCase("Test");
    }

    @Test
    void searchByTechnology_ShouldReturnMatchingProjects() {
        // Arrange
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findByTechnology("Java")).thenReturn(projects);

        // Act
        List<ProjectResponse> result = projectService.searchByTechnology("Java");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTechStack()).contains("Java");
        verify(projectRepository, times(1)).findByTechnology("Java");
    }
}
