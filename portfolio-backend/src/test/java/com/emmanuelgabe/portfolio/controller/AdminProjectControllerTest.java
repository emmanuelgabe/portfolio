package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminProjectController (admin endpoints).
 */
@WebMvcTest(AdminProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    private ProjectResponse testProjectResponse;
    private CreateProjectRequest createRequest;
    private UpdateProjectRequest updateRequest;

    @BeforeEach
    void setUp() {
        testProjectResponse = new ProjectResponse();
        testProjectResponse.setId(1L);
        testProjectResponse.setTitle("Test Project");
        testProjectResponse.setDescription("Test project description");
        testProjectResponse.setTechStack("Java, Spring Boot");
        testProjectResponse.setGithubUrl("https://github.com/test/project");
        testProjectResponse.setDemoUrl("https://demo.example.com");
        testProjectResponse.setImageUrl("/uploads/images/project-1.webp");
        testProjectResponse.setFeatured(true);
        testProjectResponse.setCreatedAt(LocalDateTime.now());
        testProjectResponse.setUpdatedAt(LocalDateTime.now());
        testProjectResponse.setTags(new HashSet<>());

        createRequest = new CreateProjectRequest();
        createRequest.setTitle("New Project");
        createRequest.setDescription("New project description");
        createRequest.setTechStack("Angular, TypeScript");

        updateRequest = new UpdateProjectRequest();
        updateRequest.setTitle("Updated Project");
        updateRequest.setDescription("Updated description");
    }

    // ========== Get All Projects Tests ==========

    @Test
    void should_return200AndListOfProjects_when_getAllProjectsCalled() throws Exception {
        // Arrange
        List<ProjectResponse> projects = List.of(testProjectResponse);
        when(projectService.getAllProjects()).thenReturn(projects);

        // Act & Assert
        mockMvc.perform(get("/api/admin/projects"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Project")));

        verify(projectService).getAllProjects();
    }

    @Test
    void should_return200AndEmptyList_when_getAllProjectsCalledWithNoProjects() throws Exception {
        // Arrange
        when(projectService.getAllProjects()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/projects"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(projectService).getAllProjects();
    }

    // ========== Get Project By ID Tests ==========

    @Test
    void should_return200AndProject_when_getProjectByIdCalledWithExistingId() throws Exception {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(testProjectResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/projects/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Project")))
                .andExpect(jsonPath("$.featured", is(true)));

        verify(projectService).getProjectById(1L);
    }

    @Test
    void should_return404_when_getProjectByIdCalledWithNonExistingId() throws Exception {
        // Arrange
        when(projectService.getProjectById(999L))
                .thenThrow(new ResourceNotFoundException("Project", "id", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/admin/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Project not found")));

        verify(projectService).getProjectById(999L);
    }

    // ========== Create Project Tests ==========

    @Test
    void should_return201AndCreatedProject_when_createProjectCalledWithValidRequest() throws Exception {
        // Arrange
        when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenReturn(testProjectResponse);

        // Act & Assert
        mockMvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Project")));

        verify(projectService).createProject(any(CreateProjectRequest.class));
    }

    @Test
    void should_return400_when_createProjectCalledWithInvalidRequest() throws Exception {
        // Arrange - empty request
        CreateProjectRequest invalidRequest = new CreateProjectRequest();

        // Act & Assert
        mockMvc.perform(post("/api/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ========== Update Project Tests ==========

    @Test
    void should_return200AndUpdatedProject_when_updateProjectCalledWithValidRequest() throws Exception {
        // Arrange
        ProjectResponse updatedResponse = new ProjectResponse();
        updatedResponse.setId(1L);
        updatedResponse.setTitle("Updated Project");
        updatedResponse.setDescription("Updated description");
        updatedResponse.setCreatedAt(LocalDateTime.now());
        updatedResponse.setUpdatedAt(LocalDateTime.now());
        updatedResponse.setTags(new HashSet<>());

        when(projectService.updateProject(eq(1L), any(UpdateProjectRequest.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Updated Project")));

        verify(projectService).updateProject(eq(1L), any(UpdateProjectRequest.class));
    }

    @Test
    void should_return404_when_updateProjectCalledWithNonExistingId() throws Exception {
        // Arrange
        when(projectService.updateProject(eq(999L), any(UpdateProjectRequest.class)))
                .thenThrow(new ResourceNotFoundException("Project", "id", 999L));

        // Act & Assert
        mockMvc.perform(put("/api/admin/projects/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(projectService).updateProject(eq(999L), any(UpdateProjectRequest.class));
    }

    // ========== Delete Project Tests ==========

    @Test
    void should_return204_when_deleteProjectCalledWithExistingId() throws Exception {
        // Arrange
        doNothing().when(projectService).deleteProject(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/projects/1"))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(1L);
    }

    @Test
    void should_return404_when_deleteProjectCalledWithNonExistingId() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Project", "id", 999L))
                .when(projectService).deleteProject(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(projectService).deleteProject(999L);
    }

    // ========== Image Upload Tests ==========

    @Test
    void should_return200AndImageUploadResponse_when_uploadProjectImageCalled() throws Exception {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        ImageUploadResponse uploadResponse = new ImageUploadResponse(
                "/uploads/images/project-1.webp",
                "/uploads/images/project-1-thumb.webp",
                1024L,
                LocalDateTime.now()
        );

        when(projectService.uploadAndAssignProjectImage(eq(1L), any()))
                .thenReturn(uploadResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/projects/1/image")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.imageUrl", is("/uploads/images/project-1.webp")))
                .andExpect(jsonPath("$.thumbnailUrl", is("/uploads/images/project-1-thumb.webp")));

        verify(projectService).uploadAndAssignProjectImage(eq(1L), any());
    }

    @Test
    void should_return204_when_deleteProjectImageCalledWithExistingId() throws Exception {
        // Arrange
        doNothing().when(projectService).deleteProjectImage(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/projects/1/image"))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProjectImage(1L);
    }
}
