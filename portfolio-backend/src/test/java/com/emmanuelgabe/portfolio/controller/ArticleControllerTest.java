package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ArticleController (public endpoints only).
 * Admin endpoint tests are in AdminArticleControllerTest.
 */
@WebMvcTest(ArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleService articleService;

    private ArticleResponse testArticleResponse;

    @BeforeEach
    void setUp() {
        testArticleResponse = new ArticleResponse();
        testArticleResponse.setId(1L);
        testArticleResponse.setTitle("Test Article");
        testArticleResponse.setSlug("test-article");
        testArticleResponse.setContent("Test content");
        testArticleResponse.setExcerpt("Test excerpt");
        testArticleResponse.setDraft(false);
        testArticleResponse.setPublishedAt(LocalDateTime.now());
        testArticleResponse.setReadingTimeMinutes(5);
        testArticleResponse.setTags(new ArrayList<>());
    }

    @Test
    void should_return200AndListOfPublishedArticles_when_getAllPublished() throws Exception {
        List<ArticleResponse> articles = List.of(testArticleResponse);
        when(articleService.getAllPublished()).thenReturn(articles);

        mockMvc.perform(get("/api/articles"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(1)))
            .andExpect(jsonPath("$[0].title", is("Test Article")));

        verify(articleService).getAllPublished();
    }

    @Test
    void should_return200AndArticle_when_getBySlugWithExistingSlug() throws Exception {
        when(articleService.getBySlug("test-article")).thenReturn(testArticleResponse);

        mockMvc.perform(get("/api/articles/test-article"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.title", is("Test Article")))
            .andExpect(jsonPath("$.slug", is("test-article")));

        verify(articleService).getBySlug("test-article");
    }

    @Test
    void should_return404_when_getBySlugWithNonexistentSlug() throws Exception {
        when(articleService.getBySlug("nonexistent"))
            .thenThrow(new ResourceNotFoundException("Article not found with slug: nonexistent"));

        mockMvc.perform(get("/api/articles/nonexistent"))
            .andExpect(status().isNotFound());

        verify(articleService).getBySlug("nonexistent");
    }

    // ========== Paginated Endpoint Tests ==========

    @Test
    void should_return200AndPageOfArticles_when_getAllPublishedPaginated() throws Exception {
        // Arrange
        Page<ArticleResponse> articlePage = new PageImpl<>(
            List.of(testArticleResponse),
            PageRequest.of(0, 10),
            1
        );
        when(articleService.getAllPublishedPaginated(any())).thenReturn(articlePage);

        // Act / Assert
        mockMvc.perform(get("/api/articles/paginated")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id", is(1)))
            .andExpect(jsonPath("$.content[0].title", is("Test Article")))
            .andExpect(jsonPath("$.totalElements", is(1)))
            .andExpect(jsonPath("$.totalPages", is(1)));

        verify(articleService).getAllPublishedPaginated(any());
    }

    @Test
    void should_return200AndEmptyPage_when_getAllPublishedPaginatedWithNoArticles() throws Exception {
        // Arrange
        Page<ArticleResponse> emptyPage = new PageImpl<>(
            List.of(),
            PageRequest.of(0, 10),
            0
        );
        when(articleService.getAllPublishedPaginated(any())).thenReturn(emptyPage);

        // Act / Assert
        mockMvc.perform(get("/api/articles/paginated"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements", is(0)));

        verify(articleService).getAllPublishedPaginated(any());
    }
}
