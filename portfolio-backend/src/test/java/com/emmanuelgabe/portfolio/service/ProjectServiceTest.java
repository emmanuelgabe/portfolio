package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.ReorderProjectImagesRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectImageRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.ProjectImageMapper;
import com.emmanuelgabe.portfolio.mapper.ProjectMapper;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectImageRepository projectImageRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectImageMapper projectImageMapper;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project testProject;
    private Tag testTag;
    private ProjectResponse testProjectResponse;

    @BeforeEach
    void setUp() {
        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("Java");
        testTag.setColor("#FF5733");

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

        // Create test ProjectResponse
        testProjectResponse = new ProjectResponse();
        testProjectResponse.setId(1L);
        testProjectResponse.setTitle("Test Project");
        testProjectResponse.setDescription("Test Description for project");
        testProjectResponse.setTechStack("Java, Spring Boot");
        testProjectResponse.setGithubUrl("https://github.com/test/project");
        testProjectResponse.setImageUrl("https://example.com/image.jpg");
        testProjectResponse.setDemoUrl("https://example.com/demo");
        testProjectResponse.setFeatured(true);
        testProjectResponse.setCreatedAt(testProject.getCreatedAt());
        testProjectResponse.setUpdatedAt(testProject.getUpdatedAt());

        // Mock projectMapper behavior (using lenient to avoid UnnecessaryStubbingException)
        // Use thenAnswer to dynamically create responses based on input Project
        lenient().when(projectMapper.toResponse(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            ProjectResponse response = new ProjectResponse();
            response.setId(project.getId());
            response.setTitle(project.getTitle());
            response.setDescription(project.getDescription());
            response.setTechStack(project.getTechStack());
            response.setGithubUrl(project.getGithubUrl());
            response.setImageUrl(project.getImageUrl());
            response.setDemoUrl(project.getDemoUrl());
            response.setFeatured(project.isFeatured());
            response.setCreatedAt(project.getCreatedAt());
            response.setUpdatedAt(project.getUpdatedAt());
            if (project.getTags() != null && !project.getTags().isEmpty()) {
                response.setTags(project.getTags().stream()
                    .map(tag -> new com.emmanuelgabe.portfolio.dto.TagResponse(tag.getId(), tag.getName(), tag.getColor()))
                    .collect(java.util.stream.Collectors.toSet()));
            }
            return response;
        });
        lenient().when(projectMapper.toEntity(any(CreateProjectRequest.class))).thenReturn(testProject);
    }

    @Test
    void should_returnListOfProjects_when_getAllProjectsCalled() {
        // Arrange
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findAllWithTags()).thenReturn(projects);

        // Act
        List<ProjectResponse> result = projectService.getAllProjects();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Project");
        verify(projectRepository, times(1)).findAllWithTags();
    }

    @Test
    void should_returnProject_when_getProjectByIdCalledWithExistingProject() {
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
    void should_throwException_when_getProjectByIdCalledWithNonExistentProject() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
        verify(projectRepository, times(1)).findById(999L);
    }

    @Test
    void should_returnCreatedProject_when_createProjectCalledWithoutTags() {
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
        assertThat(result.isFeatured()).isFalse();
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void should_returnCreatedProjectWithTags_when_createProjectCalledWithTags() {
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
    void should_throwException_when_createProjectCalledWithInvalidTagId() {
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
    void should_returnUpdatedProject_when_updateProjectCalledWithExistingProject() {
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
    void should_throwException_when_updateProjectCalledWithNonExistentProject() {
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
    void should_updateTags_when_updateProjectCalledWithTags() {
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
    void should_deleteProject_when_deleteProjectCalledWithExistingProject() {
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
    void should_throwException_when_deleteProjectCalledWithNonExistentProject() {
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
    void should_returnFeaturedProjects_when_getFeaturedProjectsCalled() {
        // Arrange
        List<Project> featuredProjects = Arrays.asList(testProject);
        when(projectRepository.findFeaturedWithTags()).thenReturn(featuredProjects);

        // Act
        List<ProjectResponse> result = projectService.getFeaturedProjects();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isFeatured()).isTrue();
        verify(projectRepository, times(1)).findFeaturedWithTags();
    }

    @Test
    void should_returnMatchingProjects_when_searchByTitleCalled() {
        // Arrange
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findByTitleWithTags("Test")).thenReturn(projects);

        // Act
        List<ProjectResponse> result = projectService.searchByTitle("Test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).contains("Test");
        verify(projectRepository, times(1)).findByTitleWithTags("Test");
    }

    @Test
    void should_returnMatchingProjects_when_searchByTechnologyCalled() {
        // Arrange
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findByTechnologyWithTags("Java")).thenReturn(projects);

        // Act
        List<ProjectResponse> result = projectService.searchByTechnology("Java");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTechStack()).contains("Java");
        verify(projectRepository, times(1)).findByTechnologyWithTags("Java");
    }

    @Test
    void should_updateImageUrls_when_updateImageUrlsCalled() {
        // Arrange
        String imageUrl = "https://example.com/new-image.jpg";
        String thumbnailUrl = "https://example.com/new-thumbnail.jpg";

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        projectService.updateImageUrls(1L, imageUrl, thumbnailUrl);

        // Assert
        verify(projectRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void should_throwException_when_updateImageUrlsCalledWithNonExistentProject() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.updateImageUrls(999L, "url", "thumbnail"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
        verify(projectRepository, times(1)).findById(999L);
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void should_uploadAndAssignImage_when_uploadAndAssignProjectImageCalled() {
        // Arrange
        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");

        ImageUploadResponse uploadResponse = new ImageUploadResponse();
        uploadResponse.setImageUrl("https://example.com/image.jpg");
        uploadResponse.setThumbnailUrl("https://example.com/thumbnail.jpg");

        when(imageService.uploadProjectImage(1L, mockFile)).thenReturn(uploadResponse);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        ImageUploadResponse result = projectService.uploadAndAssignProjectImage(1L, mockFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.getThumbnailUrl()).isEqualTo("https://example.com/thumbnail.jpg");
        verify(imageService, times(1)).uploadProjectImage(1L, mockFile);
        verify(projectRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void should_deleteImage_when_deleteProjectImageCalled() {
        // Arrange
        doNothing().when(imageService).deleteProjectImage(1L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        projectService.deleteProjectImage(1L);

        // Assert
        verify(imageService, times(1)).deleteProjectImage(1L);
        verify(projectRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    // ========== HasDetails Validation Tests ==========

    @Test
    void should_throwException_when_createProjectWithHasDetailsTrueAndNoTechStack() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("New Project");
        request.setDescription("A description that is long enough");
        request.setHasDetails(true);
        request.setTechStack(null); // No tech stack

        // Act & Assert
        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tech stack is required when project has details page");
    }

    @Test
    void should_throwException_when_createProjectWithHasDetailsTrueAndBlankTechStack() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("New Project");
        request.setDescription("A description that is long enough");
        request.setHasDetails(true);
        request.setTechStack("   "); // Blank tech stack

        // Act & Assert
        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tech stack is required when project has details page");
    }

    @Test
    void should_throwException_when_updateProjectWithHasDetailsTrueAndNoTechStack() {
        // Arrange
        Project existingProject = new Project();
        existingProject.setId(1L);
        existingProject.setTitle("Existing Project");
        existingProject.setDescription("Existing description");
        existingProject.setHasDetails(false);
        existingProject.setTechStack(null);
        existingProject.setTags(new HashSet<>());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setHasDetails(true);
        // TechStack remains null

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));

        // Act & Assert
        assertThatThrownBy(() -> projectService.updateProject(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tech stack is required when project has details page");
    }

    @Test
    void should_createProject_when_hasDetailsFalseWithNoTechStack() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("New Project");
        request.setDescription("A description that is long enough");
        request.setHasDetails(false); // No details page
        request.setTechStack(null); // No tech stack - should be allowed

        Project savedProject = new Project();
        savedProject.setId(2L);
        savedProject.setTitle(request.getTitle());
        savedProject.setDescription(request.getDescription());
        savedProject.setHasDetails(false);
        savedProject.setFeatured(false);
        savedProject.setTags(new HashSet<>());

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        // Act
        ProjectResponse result = projectService.createProject(request);

        // Assert
        assertThat(result).isNotNull();
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    // ========== Multi-Image Management Tests ==========

    @Test
    void should_addImageToProject_when_validRequest() {
        // Arrange
        Long projectId = 1L;
        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(projectImageRepository.countByProjectId(projectId)).thenReturn(0);

        ImageUploadResponse uploadResponse = new ImageUploadResponse();
        uploadResponse.setImageUrl("/uploads/projects/project_1_img0.webp");
        uploadResponse.setThumbnailUrl("/uploads/projects/project_1_img0_thumb.webp");
        when(imageService.uploadProjectCarouselImage(eq(projectId), eq(0), any())).thenReturn(uploadResponse);

        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectImageRepository.findByProjectIdOrderByDisplayOrderAsc(projectId)).thenReturn(new ArrayList<>());

        ProjectImageResponse expectedResponse = new ProjectImageResponse();
        expectedResponse.setId(1L);
        expectedResponse.setImageUrl(uploadResponse.getImageUrl());
        when(projectImageMapper.toResponse(any(ProjectImage.class))).thenReturn(expectedResponse);

        // Act
        ProjectImageResponse result = projectService.addImageToProject(projectId, mockFile, "alt text", "caption");

        // Assert
        assertThat(result).isNotNull();
        verify(imageService, times(1)).uploadProjectCarouselImage(eq(projectId), eq(0), any());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void should_throwException_when_addImageToProjectExceedsLimit() {
        // Arrange
        Long projectId = 1L;
        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(projectImageRepository.countByProjectId(projectId)).thenReturn(10); // Already at limit

        // Act & Assert
        assertThatThrownBy(() -> projectService.addImageToProject(projectId, mockFile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maximum 10 images allowed");
    }

    @Test
    void should_throwException_when_addImageToNonExistentProject() {
        // Arrange
        Long projectId = 999L;
        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.addImageToProject(projectId, mockFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void should_removeImageFromProject_when_validRequest() {
        // Arrange
        Long projectId = 1L;
        Long imageId = 10L;

        ProjectImage projectImage = new ProjectImage(testProject, "/uploads/image.webp", "/uploads/thumb.webp");
        projectImage.setId(imageId);
        projectImage.setDisplayOrder(0);
        projectImage.setPrimary(false);

        testProject.getImages().add(projectImage);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        doNothing().when(imageService).deleteProjectImageByUrl(any());
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        doNothing().when(projectImageRepository).decrementOrderAfter(anyLong(), anyInt());

        // Act
        projectService.removeImageFromProject(projectId, imageId);

        // Assert
        verify(imageService, times(1)).deleteProjectImageByUrl(any());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void should_throwException_when_removeImageFromNonExistentProject() {
        // Arrange
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.removeImageFromProject(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void should_throwException_when_removeNonExistentImage() {
        // Arrange
        Long projectId = 1L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

        // Act & Assert
        assertThatThrownBy(() -> projectService.removeImageFromProject(projectId, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ProjectImage not found");
    }

    @Test
    void should_updateProjectImage_when_validRequest() {
        // Arrange
        Long projectId = 1L;
        Long imageId = 10L;

        ProjectImage projectImage = new ProjectImage(testProject, "/uploads/image.webp", "/uploads/thumb.webp");
        projectImage.setId(imageId);

        UpdateProjectImageRequest request = new UpdateProjectImageRequest();
        request.setAltText("Updated alt text");
        request.setCaption("Updated caption");

        when(projectImageRepository.findByIdAndProjectId(imageId, projectId)).thenReturn(Optional.of(projectImage));
        when(projectImageRepository.save(any(ProjectImage.class))).thenReturn(projectImage);

        ProjectImageResponse expectedResponse = new ProjectImageResponse();
        expectedResponse.setId(imageId);
        expectedResponse.setAltText("Updated alt text");
        when(projectImageMapper.toResponse(any(ProjectImage.class))).thenReturn(expectedResponse);

        // Act
        ProjectImageResponse result = projectService.updateProjectImage(projectId, imageId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAltText()).isEqualTo("Updated alt text");
        verify(projectImageRepository, times(1)).save(any(ProjectImage.class));
    }

    @Test
    void should_throwException_when_updateNonExistentProjectImage() {
        // Arrange
        when(projectImageRepository.findByIdAndProjectId(999L, 1L)).thenReturn(Optional.empty());

        UpdateProjectImageRequest request = new UpdateProjectImageRequest();
        request.setAltText("New alt text");

        // Act & Assert
        assertThatThrownBy(() -> projectService.updateProjectImage(1L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ProjectImage not found");
    }

    @Test
    void should_setPrimaryImage_when_validRequest() {
        // Arrange
        Long projectId = 1L;
        Long imageId = 10L;

        ProjectImage projectImage = new ProjectImage(testProject, "/uploads/image.webp", "/uploads/thumb.webp");
        projectImage.setId(imageId);
        projectImage.setPrimary(false);

        when(projectImageRepository.findByIdAndProjectId(imageId, projectId)).thenReturn(Optional.of(projectImage));
        doNothing().when(projectImageRepository).clearPrimaryForProject(projectId);
        when(projectImageRepository.save(any(ProjectImage.class))).thenReturn(projectImage);

        // Act
        projectService.setPrimaryImage(projectId, imageId);

        // Assert
        verify(projectImageRepository, times(1)).clearPrimaryForProject(projectId);
        verify(projectImageRepository, times(1)).save(any(ProjectImage.class));
    }

    @Test
    void should_throwException_when_setPrimaryOnNonExistentImage() {
        // Arrange
        when(projectImageRepository.findByIdAndProjectId(999L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.setPrimaryImage(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ProjectImage not found");
    }

    @Test
    void should_reorderImages_when_validRequest() {
        // Arrange
        Long projectId = 1L;

        ProjectImage image1 = new ProjectImage(testProject, "/uploads/img1.webp", "/uploads/thumb1.webp");
        image1.setId(1L);
        image1.setDisplayOrder(0);

        ProjectImage image2 = new ProjectImage(testProject, "/uploads/img2.webp", "/uploads/thumb2.webp");
        image2.setId(2L);
        image2.setDisplayOrder(1);

        testProject.getImages().add(image1);
        testProject.getImages().add(image2);

        ReorderProjectImagesRequest request = new ReorderProjectImagesRequest();
        request.setImageIds(Arrays.asList(2L, 1L)); // Reverse order

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        projectService.reorderImages(projectId, request);

        // Assert
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void should_throwException_when_reorderImagesOnNonExistentProject() {
        // Arrange
        ReorderProjectImagesRequest request = new ReorderProjectImagesRequest();
        request.setImageIds(Arrays.asList(1L, 2L));

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.reorderImages(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void should_throwException_when_reorderWithInvalidImageId() {
        // Arrange
        Long projectId = 1L;

        ProjectImage image1 = new ProjectImage(testProject, "/uploads/img1.webp", "/uploads/thumb1.webp");
        image1.setId(1L);
        testProject.getImages().add(image1);

        ReorderProjectImagesRequest request = new ReorderProjectImagesRequest();
        request.setImageIds(Arrays.asList(1L, 999L)); // 999L doesn't exist

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

        // Act & Assert
        assertThatThrownBy(() -> projectService.reorderImages(projectId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ProjectImage not found");
    }

    @Test
    void should_getProjectImages_when_projectExists() {
        // Arrange
        Long projectId = 1L;

        ProjectImage image1 = new ProjectImage(testProject, "/uploads/img1.webp", "/uploads/thumb1.webp");
        image1.setId(1L);

        ProjectImage image2 = new ProjectImage(testProject, "/uploads/img2.webp", "/uploads/thumb2.webp");
        image2.setId(2L);

        List<ProjectImage> images = Arrays.asList(image1, image2);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectImageRepository.findByProjectIdOrderByDisplayOrderAsc(projectId)).thenReturn(images);

        ProjectImageResponse response1 = new ProjectImageResponse();
        response1.setId(1L);
        ProjectImageResponse response2 = new ProjectImageResponse();
        response2.setId(2L);

        when(projectImageMapper.toResponseList(images)).thenReturn(Arrays.asList(response1, response2));

        // Act
        List<ProjectImageResponse> result = projectService.getProjectImages(projectId);

        // Assert
        assertThat(result).hasSize(2);
        verify(projectImageRepository, times(1)).findByProjectIdOrderByDisplayOrderAsc(projectId);
    }

    @Test
    void should_throwException_when_getProjectImagesOnNonExistentProject() {
        // Arrange
        when(projectRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> projectService.getProjectImages(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
    }
}
