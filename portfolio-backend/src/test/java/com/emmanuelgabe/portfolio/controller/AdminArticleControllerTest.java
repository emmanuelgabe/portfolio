package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.dto.article.CreateArticleRequest;
import com.emmanuelgabe.portfolio.dto.article.UpdateArticleRequest;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminArticleController (admin endpoints).
 */
@WebMvcTest(AdminArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ArticleService articleService;

    private ArticleResponse testArticleResponse;
    private CreateArticleRequest createRequest;
    private UpdateArticleRequest updateRequest;

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

        createRequest = new CreateArticleRequest();
        createRequest.setTitle("New Article");
        createRequest.setContent("New content");

        updateRequest = new UpdateArticleRequest();
        updateRequest.setTitle("Updated Article");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndAllArticles_when_getAllArticles() throws Exception {
        List<ArticleResponse> articles = List.of(testArticleResponse);
        when(articleService.getAllArticles()).thenReturn(articles);

        mockMvc.perform(get("/api/admin/articles"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)));

        verify(articleService).getAllArticles();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndArticle_when_getById() throws Exception {
        when(articleService.getById(1L)).thenReturn(testArticleResponse);

        mockMvc.perform(get("/api/admin/articles/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.title", is("Test Article")));

        verify(articleService).getById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_getByIdWithNonexistentId() throws Exception {
        when(articleService.getById(999L))
            .thenThrow(new ResourceNotFoundException("Article not found with ID: 999"));

        mockMvc.perform(get("/api/admin/articles/999"))
            .andExpect(status().isNotFound());

        verify(articleService).getById(999L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void should_return201AndCreatedArticle_when_createArticleWithValidRequest() throws Exception {
        when(articleService.createArticle(any(CreateArticleRequest.class), eq("admin")))
            .thenReturn(testArticleResponse);

        mockMvc.perform(post("/api/admin/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.title", is("Test Article")));

        verify(articleService).createArticle(any(CreateArticleRequest.class), eq("admin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void should_return400_when_createArticleWithInvalidRequest() throws Exception {
        CreateArticleRequest invalidRequest = new CreateArticleRequest();

        mockMvc.perform(post("/api/admin/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(articleService, never()).createArticle(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndUpdatedArticle_when_updateArticleWithValidRequest() throws Exception {
        when(articleService.updateArticle(eq(1L), any(UpdateArticleRequest.class)))
            .thenReturn(testArticleResponse);

        mockMvc.perform(put("/api/admin/articles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)));

        verify(articleService).updateArticle(eq(1L), any(UpdateArticleRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_updateNonexistentArticle() throws Exception {
        when(articleService.updateArticle(eq(999L), any(UpdateArticleRequest.class)))
            .thenThrow(new ResourceNotFoundException("Article not found with ID: 999"));

        mockMvc.perform(put("/api/admin/articles/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isNotFound());

        verify(articleService).updateArticle(eq(999L), any(UpdateArticleRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return204_when_deleteArticleWithExistingId() throws Exception {
        mockMvc.perform(delete("/api/admin/articles/1"))
            .andExpect(status().isNoContent());

        verify(articleService).deleteArticle(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_deleteNonexistentArticle() throws Exception {
        doThrow(new ResourceNotFoundException("Article not found with ID: 999"))
            .when(articleService).deleteArticle(999L);

        mockMvc.perform(delete("/api/admin/articles/999"))
            .andExpect(status().isNotFound());

        verify(articleService).deleteArticle(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndPublishedArticle_when_publishArticle() throws Exception {
        when(articleService.publishArticle(1L)).thenReturn(testArticleResponse);

        mockMvc.perform(put("/api/admin/articles/1/publish"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)));

        verify(articleService).publishArticle(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndUnpublishedArticle_when_unpublishArticle() throws Exception {
        testArticleResponse.setDraft(true);
        when(articleService.unpublishArticle(1L)).thenReturn(testArticleResponse);

        mockMvc.perform(put("/api/admin/articles/1/unpublish"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)));

        verify(articleService).unpublishArticle(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_publishNonexistentArticle() throws Exception {
        when(articleService.publishArticle(999L))
            .thenThrow(new ResourceNotFoundException("Article not found with ID: 999"));

        mockMvc.perform(put("/api/admin/articles/999/publish"))
            .andExpect(status().isNotFound());

        verify(articleService).publishArticle(999L);
    }
}
