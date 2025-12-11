package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ProjectController (public endpoints only).
 * Admin endpoint tests are in AdminProjectControllerTest.
 */
@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private BusinessMetrics metrics;

    private ProjectResponse testProjectResponse;

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
    }

    @Test
    void should_return200AndListOfProjects_when_getAllProjectsCalled() throws Exception {
        List<ProjectResponse> projects = List.of(testProjectResponse);
        when(projectService.getAllProjects()).thenReturn(projects);

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Project")))
                .andExpect(jsonPath("$[0].techStack", is("Java, Spring Boot")))
                .andExpect(jsonPath("$[0].featured", is(true)));

        verify(projectService).getAllProjects();
    }

    @Test
    void should_return200AndProject_when_getProjectByIdCalledWithExistingProject() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(testProjectResponse);

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Project")))
                .andExpect(jsonPath("$.description", is("Test Description for the project")));

        verify(projectService).getProjectById(1L);
        verify(metrics).recordProjectView();
    }

    @Test
    void should_return404_when_getProjectByIdCalledWithNonExistentProject() throws Exception {
        when(projectService.getProjectById(999L))
                .thenThrow(new ResourceNotFoundException("Project", "id", 999L));

        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(projectService).getProjectById(999L);
    }

    @Test
    void should_return200AndListOfFeaturedProjects_when_getFeaturedProjectsCalled() throws Exception {
        List<ProjectResponse> featuredProjects = List.of(testProjectResponse);
        when(projectService.getFeaturedProjects()).thenReturn(featuredProjects);

        mockMvc.perform(get("/api/projects/featured"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].featured", is(true)));

        verify(projectService).getFeaturedProjects();
    }

    @Test
    void should_return200AndMatchingProjects_when_searchByTitleCalled() throws Exception {
        List<ProjectResponse> projects = List.of(testProjectResponse);
        when(projectService.searchByTitle("Test")).thenReturn(projects);

        mockMvc.perform(get("/api/projects/search/title")
                        .param("title", "Test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Test")));

        verify(projectService).searchByTitle("Test");
    }

    @Test
    void should_return200AndMatchingProjects_when_searchByTechnologyCalled() throws Exception {
        List<ProjectResponse> projects = List.of(testProjectResponse);
        when(projectService.searchByTechnology("Java")).thenReturn(projects);

        mockMvc.perform(get("/api/projects/search/technology")
                        .param("technology", "Java"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].techStack", containsString("Java")));

        verify(projectService).searchByTechnology("Java");
    }
}
