package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleSearchToolTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private ToolCallTracker tracker;

    @InjectMocks
    private ArticleSearchTool articleSearchTool;

    // ========== listArticles ==========

    @Test
    void should_returnPaginatedArticles_when_listArticlesCalled() {
        // Arrange
        ArticleResponse article = new ArticleResponse();
        article.setTitle("Spring AI Guide");
        Page<ArticleResponse> page = new PageImpl<>(List.of(article));
        when(articleService.getAllPublishedPaginated(any(Pageable.class))).thenReturn(page);

        // Act
        Page<ArticleResponse> result = articleSearchTool.listArticles(0, 5);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring AI Guide");
    }

    @Test
    void should_capPageSizeAt10_when_sizeExceedsMax() {
        // Arrange
        when(articleService.getAllPublishedPaginated(any(Pageable.class)))
                .thenReturn(Page.empty());

        // Act
        articleSearchTool.listArticles(0, 50);

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(articleService).getAllPublishedPaginated(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void should_enforceMinPageSize_when_sizeIsZeroOrNegative() {
        // Arrange
        when(articleService.getAllPublishedPaginated(any(Pageable.class)))
                .thenReturn(Page.empty());

        // Act
        articleSearchTool.listArticles(0, 0);

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(articleService).getAllPublishedPaginated(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
    }

    // ========== getArticle ==========

    @Test
    void should_returnArticle_when_getArticleCalledWithSlug() {
        // Arrange
        ArticleResponse article = new ArticleResponse();
        article.setSlug("spring-ai-guide");
        article.setTitle("Spring AI Guide");
        when(articleService.getBySlug("spring-ai-guide")).thenReturn(article);

        // Act
        ArticleResponse result = articleSearchTool.getArticle("spring-ai-guide");

        // Assert
        assertThat(result.getTitle()).isEqualTo("Spring AI Guide");
        verify(articleService).getBySlug("spring-ai-guide");
    }
}
