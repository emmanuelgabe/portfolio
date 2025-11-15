package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    private ProjectResponse testProjectResponse;
    private CreateProjectRequest createRequest;
    private UpdateProjectRequest updateRequest;

    @BeforeEach
    void setUp() {
        testProjectResponse = new ProjectResponse();
        testProjectResponse.setId(1L);
        testProjectResponse.setTitle("Test Project");
        testProjectResponse.setDescription("Test Description for the project");
        testProjectResponse.setTechStack("Java, Spring Boot");
        testProjectResponse.setGithubUrl("https://github.com/test/project");
        testProjectResponse.setImageUrl("https://example.com/image.jpg");
        testProjectResponse.setDemoUrl("https://example.com/demo");
        testProjectResponse.setFeatured(true);
        testProjectResponse.setCreatedAt(LocalDateTime.now());
        testProjectResponse.setUpdatedAt(LocalDateTime.now());
        testProjectResponse.setTags(new HashSet<>());

        createRequest = new CreateProjectRequest();
        createRequest.setTitle("New Project");
        createRequest.setDescription("New Description for the project");
        createRequest.setTechStack("Java, Spring");

        updateRequest = new UpdateProjectRequest();
        updateRequest.setTitle("Updated Project");
    }

    @Test
    void getAllProjects_ShouldReturn200AndListOfProjects() throws Exception {
        // Arrange
        List<ProjectResponse> projects = Arrays.asList(testProjectResponse);
        when(projectService.getAllProjects()).thenReturn(projects);

        // Act & Assert
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Project")))
                .andExpect(jsonPath("$[0].techStack", is("Java, Spring Boot")))
                .andExpect(jsonPath("$[0].featured", is(true)));

        verify(projectService, times(1)).getAllProjects();
    }

    @Test
    void getProjectById_WhenExists_ShouldReturn200AndProject() throws Exception {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(testProjectResponse);

        // Act & Assert
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Project")))
                .andExpect(jsonPath("$.description", is("Test Description for the project")));

        verify(projectService, times(1)).getProjectById(1L);
    }

    @Test
    void getProjectById_WhenNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(projectService.getProjectById(999L))
                .thenThrow(new ResourceNotFoundException("Project", "id", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Project not found")));

        verify(projectService, times(1)).getProjectById(999L);
    }

    @Test
    void createProject_WithValidData_ShouldReturn201AndCreatedProject() throws Exception {
        // Arrange
        when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenReturn(testProjectResponse);

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Project")));

        verify(projectService, times(1)).createProject(any(CreateProjectRequest.class));
    }

    @Test
    void createProject_WithInvalidData_ShouldReturn400() throws Exception {
        // Arrange
        CreateProjectRequest invalidRequest = new CreateProjectRequest();
        invalidRequest.setTitle("Te"); // Too short
        invalidRequest.setDescription("Short"); // Too short
        invalidRequest.setTechStack(""); // Blank

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors").exists());

        verify(projectService, never()).createProject(any(CreateProjectRequest.class));
    }

    @Test
    void createProject_WithBlankTitle_ShouldReturn400() throws Exception {
        // Arrange
        CreateProjectRequest invalidRequest = new CreateProjectRequest();
        invalidRequest.setTitle(""); // Blank
        invalidRequest.setDescription("Valid description for the project");
        invalidRequest.setTechStack("Java");

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        verify(projectService, never()).createProject(any(CreateProjectRequest.class));
    }

    @Test
    void updateProject_WithValidData_ShouldReturn200AndUpdatedProject() throws Exception {
        // Arrange
        when(projectService.updateProject(eq(1L), any(UpdateProjectRequest.class)))
                .thenReturn(testProjectResponse);

        // Act & Assert
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Project")));

        verify(projectService, times(1)).updateProject(eq(1L), any(UpdateProjectRequest.class));
    }

    @Test
    void updateProject_WhenNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(projectService.updateProject(eq(999L), any(UpdateProjectRequest.class)))
                .thenThrow(new ResourceNotFoundException("Project", "id", 999L));

        // Act & Assert
        mockMvc.perform(put("/api/projects/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Project not found")));

        verify(projectService, times(1)).updateProject(eq(999L), any(UpdateProjectRequest.class));
    }

    @Test
    void deleteProject_WhenExists_ShouldReturn204() throws Exception {
        // Arrange
        doNothing().when(projectService).deleteProject(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());

        verify(projectService, times(1)).deleteProject(1L);
    }

    @Test
    void deleteProject_WhenNotFound_ShouldReturn404() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Project", "id", 999L))
                .when(projectService).deleteProject(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Project not found")));

        verify(projectService, times(1)).deleteProject(999L);
    }

    @Test
    void getFeaturedProjects_ShouldReturn200AndListOfFeaturedProjects() throws Exception {
        // Arrange
        List<ProjectResponse> featuredProjects = Arrays.asList(testProjectResponse);
        when(projectService.getFeaturedProjects()).thenReturn(featuredProjects);

        // Act & Assert
        mockMvc.perform(get("/api/projects/featured"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].featured", is(true)));

        verify(projectService, times(1)).getFeaturedProjects();
    }

    @Test
    void searchByTitle_ShouldReturn200AndMatchingProjects() throws Exception {
        // Arrange
        List<ProjectResponse> projects = Arrays.asList(testProjectResponse);
        when(projectService.searchByTitle("Test")).thenReturn(projects);

        // Act & Assert
        mockMvc.perform(get("/api/projects/search/title")
                        .param("title", "Test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Test")));

        verify(projectService, times(1)).searchByTitle("Test");
    }

    @Test
    void searchByTechnology_ShouldReturn200AndMatchingProjects() throws Exception {
        // Arrange
        List<ProjectResponse> projects = Arrays.asList(testProjectResponse);
        when(projectService.searchByTechnology("Java")).thenReturn(projects);

        // Act & Assert
        mockMvc.perform(get("/api/projects/search/technology")
                        .param("technology", "Java"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].techStack", containsString("Java")));

        verify(projectService, times(1)).searchByTechnology("Java");
    }
}
