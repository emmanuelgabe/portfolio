package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.entity.ImageStatus;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ArticleImageController.
 */
@WebMvcTest(ArticleImageController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class ArticleImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleService articleService;

    private ArticleImageResponse articleImageResponse;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        articleImageResponse = new ArticleImageResponse(
            1L,
            "/uploads/articles/article_1.webp",
            "/uploads/articles/article_1_thumb.webp",
            ImageStatus.PROCESSING,
            now
        );

        mockFile = new MockMultipartFile(
            "file",
            "test-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".getBytes()
        );
    }

    // ========== Upload Image Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return201AndImageResponse_when_uploadImageWithValidFile() throws Exception {
        // Arrange
        when(articleService.addImageToArticle(eq(1L), any())).thenReturn(articleImageResponse);

        // Act / Assert
        mockMvc.perform(multipart("/api/admin/articles/1/images")
                .file(mockFile))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.imageUrl", is("/uploads/articles/article_1.webp")))
            .andExpect(jsonPath("$.thumbnailUrl", is("/uploads/articles/article_1_thumb.webp")))
            .andExpect(jsonPath("$.status", is("PROCESSING")));

        verify(articleService).addImageToArticle(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_uploadImageToNonexistentArticle() throws Exception {
        // Arrange
        when(articleService.addImageToArticle(eq(999L), any()))
            .thenThrow(new ResourceNotFoundException("Article not found with ID: 999"));

        // Act / Assert
        mockMvc.perform(multipart("/api/admin/articles/999/images")
                .file(mockFile))
            .andExpect(status().isNotFound());

        verify(articleService).addImageToArticle(eq(999L), any());
    }

    // ========== Delete Image Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return204_when_deleteImageWithValidIds() throws Exception {
        // Arrange
        doNothing().when(articleService).removeImageFromArticle(1L, 10L);

        // Act / Assert
        mockMvc.perform(delete("/api/admin/articles/1/images/10"))
            .andExpect(status().isNoContent());

        verify(articleService).removeImageFromArticle(1L, 10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_deleteImageFromNonexistentArticle() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Article not found with ID: 999"))
            .when(articleService).removeImageFromArticle(999L, 10L);

        // Act / Assert
        mockMvc.perform(delete("/api/admin/articles/999/images/10"))
            .andExpect(status().isNotFound());

        verify(articleService).removeImageFromArticle(999L, 10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_deleteNonexistentImage() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Image not found with ID: 999"))
            .when(articleService).removeImageFromArticle(1L, 999L);

        // Act / Assert
        mockMvc.perform(delete("/api/admin/articles/1/images/999"))
            .andExpect(status().isNotFound());

        verify(articleService).removeImageFromArticle(1L, 999L);
    }
}
