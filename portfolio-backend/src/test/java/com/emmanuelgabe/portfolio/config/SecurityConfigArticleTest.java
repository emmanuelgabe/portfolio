package com.emmanuelgabe.portfolio.config;

import com.emmanuelgabe.portfolio.controller.AdminArticleController;
import com.emmanuelgabe.portfolio.controller.ArticleController;
import com.emmanuelgabe.portfolio.controller.ArticleImageController;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for Article endpoints functionality.
 * Security is disabled (addFilters = false) as this focuses on controller logic.
 * For full security integration tests, use @SpringBootTest with a test database.
 */
@WebMvcTest({ArticleController.class, AdminArticleController.class, ArticleImageController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class SecurityConfigArticleTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleService articleService;

    private ArticleResponse testArticle;

    @BeforeEach
    void setUp() {
        testArticle = new ArticleResponse();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");
        testArticle.setSlug("test-article");
        testArticle.setTags(new ArrayList<>());
    }

    // ========== Public Endpoints ==========

    @Test
    void should_return200_when_gettingPublishedArticles() throws Exception {
        when(articleService.getAllPublished()).thenReturn(List.of(testArticle));

        mockMvc.perform(get("/api/articles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Test Article"));
    }

    @Test
    void should_return200_when_gettingArticleBySlug() throws Exception {
        when(articleService.getBySlug("test-article")).thenReturn(testArticle);

        mockMvc.perform(get("/api/articles/test-article"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test Article"));
    }

    @Test
    void should_return404_when_gettingNonExistentArticleBySlug() throws Exception {
        when(articleService.getBySlug("nonexistent"))
            .thenThrow(new ResourceNotFoundException("Article not found"));

        mockMvc.perform(get("/api/articles/nonexistent"))
            .andExpect(status().isNotFound());
    }

    // ========== Admin Endpoints ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_gettingAllArticlesAdmin() throws Exception {
        when(articleService.getAllArticles()).thenReturn(List.of(testArticle));

        mockMvc.perform(get("/api/admin/articles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Test Article"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_gettingArticleByIdAdmin() throws Exception {
        when(articleService.getById(1L)).thenReturn(testArticle);

        mockMvc.perform(get("/api/admin/articles/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test Article"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void should_return201_when_creatingArticle() throws Exception {
        when(articleService.createArticle(any(), any())).thenReturn(testArticle);

        String validRequest = "{\"title\":\"Test Article\",\"content\":\"Content\"}";

        mockMvc.perform(post("/api/admin/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Test Article"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_updatingArticle() throws Exception {
        when(articleService.updateArticle(any(), any())).thenReturn(testArticle);

        String updateRequest = "{\"title\":\"Updated Article\"}";

        mockMvc.perform(put("/api/admin/articles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return204_when_deletingArticle() throws Exception {
        mockMvc.perform(delete("/api/admin/articles/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_publishingArticle() throws Exception {
        when(articleService.publishArticle(1L)).thenReturn(testArticle);

        mockMvc.perform(put("/api/admin/articles/1/publish"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_unpublishingArticle() throws Exception {
        when(articleService.unpublishArticle(1L)).thenReturn(testArticle);

        mockMvc.perform(put("/api/admin/articles/1/unpublish"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_gettingNonExistentArticleByIdAdmin() throws Exception {
        when(articleService.getById(999L))
            .thenThrow(new ResourceNotFoundException("Article not found"));

        mockMvc.perform(get("/api/admin/articles/999"))
            .andExpect(status().isNotFound());
    }
}
