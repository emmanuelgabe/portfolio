package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.search.ArticleSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ExperienceSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminSearchController.
 */
@WebMvcTest(AdminSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private BusinessMetrics metrics;

    private ArticleSearchResult testArticleResult;
    private ProjectSearchResult testProjectResult;
    private ExperienceSearchResult testExperienceResult;

    @BeforeEach
    void setUp() {
        testArticleResult = new ArticleSearchResult();
        testArticleResult.setId(1L);
        testArticleResult.setTitle("Test Article");
        testArticleResult.setSlug("test-article");
        testArticleResult.setExcerpt("Test excerpt");
        testArticleResult.setDraft(false);
        testArticleResult.setPublishedAt(LocalDateTime.now());
        testArticleResult.setTags(List.of("Java", "Spring"));

        testProjectResult = new ProjectSearchResult();
        testProjectResult.setId(1L);
        testProjectResult.setTitle("Test Project");
        testProjectResult.setDescription("Test description");
        testProjectResult.setTechStack("Java, Spring Boot");
        testProjectResult.setFeatured(true);
        testProjectResult.setTags(List.of("Backend"));

        testExperienceResult = new ExperienceSearchResult();
        testExperienceResult.setId(1L);
        testExperienceResult.setCompany("Tech Company");
        testExperienceResult.setRole("Developer");
        testExperienceResult.setType("WORK");
        testExperienceResult.setStartDate(LocalDate.of(2020, 1, 1));
        testExperienceResult.setEndDate(LocalDate.of(2023, 12, 31));
    }

    // ========== Search Articles Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnArticles_when_searchArticlesCalledWithValidQuery() throws Exception {
        // Arrange
        String query = "test";
        when(searchService.searchArticles(query)).thenReturn(List.of(testArticleResult));

        // Act & Assert
        mockMvc.perform(get("/api/admin/search/articles")
                        .param("q", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Article")))
                .andExpect(jsonPath("$[0].slug", is("test-article")));

        verify(searchService).searchArticles(query);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnEmptyList_when_searchArticlesCalledWithNoResults() throws Exception {
        // Arrange
        String query = "nonexistent";
        when(searchService.searchArticles(query)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/search/articles")
                        .param("q", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(searchService).searchArticles(query);
    }


    // ========== Search Projects Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnProjects_when_searchProjectsCalledWithValidQuery() throws Exception {
        // Arrange
        String query = "test";
        when(searchService.searchProjects(query)).thenReturn(List.of(testProjectResult));

        // Act & Assert
        mockMvc.perform(get("/api/admin/search/projects")
                        .param("q", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Project")))
                .andExpect(jsonPath("$[0].techStack", is("Java, Spring Boot")));

        verify(searchService).searchProjects(query);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnEmptyList_when_searchProjectsCalledWithNoResults() throws Exception {
        // Arrange
        String query = "nonexistent";
        when(searchService.searchProjects(query)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/search/projects")
                        .param("q", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(searchService).searchProjects(query);
    }

    // ========== Search Experiences Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnExperiences_when_searchExperiencesCalledWithValidQuery() throws Exception {
        // Arrange
        String query = "developer";
        when(searchService.searchExperiences(query)).thenReturn(List.of(testExperienceResult));

        // Act & Assert
        mockMvc.perform(get("/api/admin/search/experiences")
                        .param("q", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].company", is("Tech Company")))
                .andExpect(jsonPath("$[0].role", is("Developer")));

        verify(searchService).searchExperiences(query);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnEmptyList_when_searchExperiencesCalledWithNoResults() throws Exception {
        // Arrange
        String query = "nonexistent";
        when(searchService.searchExperiences(query)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/search/experiences")
                        .param("q", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(searchService).searchExperiences(query);
    }

    // ========== Reindex Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnReindexResponse_when_reindexAllCalled() throws Exception {
        // Arrange
        when(searchService.reindexAll()).thenReturn(100);

        // Act & Assert
        mockMvc.perform(post("/api/admin/search/reindex"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalIndexed", is(100)))
                .andExpect(jsonPath("$.message", is("Reindex completed successfully")));

        verify(searchService).reindexAll();
    }
}
