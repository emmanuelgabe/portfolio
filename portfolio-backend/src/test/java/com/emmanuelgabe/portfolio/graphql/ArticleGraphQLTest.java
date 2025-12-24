package com.emmanuelgabe.portfolio.graphql;

import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.graphql.config.GraphQLConfig;
import com.emmanuelgabe.portfolio.graphql.resolver.query.ArticleQueryResolver;
import com.emmanuelgabe.portfolio.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * GraphQL integration tests for Article queries.
 */
@GraphQlTest(ArticleQueryResolver.class)
@Import(GraphQLConfig.class)
class ArticleGraphQLTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private ArticleService articleService;

    // ========== articles Query Tests ==========

    @Test
    void should_returnArticles_when_articlesQueryExecuted() {
        // Arrange
        ArticleResponse article = createTestArticle();
        when(articleService.getAllPublished()).thenReturn(List.of(article));

        // Act & Assert
        graphQlTester.document("""
                    query {
                        articles {
                            edges {
                                node {
                                    id
                                    title
                                    slug
                                }
                            }
                            totalCount
                        }
                    }
                """)
                .execute()
                .path("articles.totalCount")
                .entity(Long.class)
                .isEqualTo(1L)
                .path("articles.edges[0].node.title")
                .entity(String.class)
                .isEqualTo("Test Article");
    }

    @Test
    void should_returnEmptyConnection_when_noArticlesExist() {
        // Arrange
        when(articleService.getAllPublished()).thenReturn(List.of());

        // Act & Assert
        graphQlTester.document("""
                    query {
                        articles {
                            edges {
                                node {
                                    id
                                }
                            }
                            totalCount
                        }
                    }
                """)
                .execute()
                .path("articles.totalCount")
                .entity(Long.class)
                .isEqualTo(0L)
                .path("articles.edges")
                .entityList(Object.class)
                .hasSize(0);
    }

    // ========== articleBySlug Query Tests ==========

    @Test
    void should_returnArticle_when_articleBySlugQueryExecutedWithValidSlug() {
        // Arrange
        ArticleResponse article = createTestArticle();
        when(articleService.getBySlug("test-article")).thenReturn(article);

        // Act & Assert
        graphQlTester.document("""
                    query {
                        articleBySlug(slug: "test-article") {
                            id
                            title
                            slug
                            content
                            authorName
                        }
                    }
                """)
                .execute()
                .path("articleBySlug.id")
                .entity(Long.class)
                .isEqualTo(1L)
                .path("articleBySlug.slug")
                .entity(String.class)
                .isEqualTo("test-article");
    }

    // ========== article Query Tests ==========

    @Test
    void should_returnArticle_when_articleQueryExecutedWithValidId() {
        // Arrange
        ArticleResponse article = createTestArticle();
        when(articleService.getById(1L)).thenReturn(article);

        // Act & Assert
        graphQlTester.document("""
                    query {
                        article(id: 1) {
                            id
                            title
                            content
                            contentHtml
                            tags {
                                id
                                name
                            }
                        }
                    }
                """)
                .execute()
                .path("article.id")
                .entity(Long.class)
                .isEqualTo(1L)
                .path("article.title")
                .entity(String.class)
                .isEqualTo("Test Article");
    }

    private ArticleResponse createTestArticle() {
        ArticleResponse article = new ArticleResponse();
        article.setId(1L);
        article.setTitle("Test Article");
        article.setSlug("test-article");
        article.setContent("# Test Content");
        article.setContentHtml("<h1>Test Content</h1>");
        article.setExcerpt("Test excerpt");
        article.setAuthorName("Test Author");
        article.setDraft(false);
        article.setPublishedAt(LocalDateTime.now());
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        article.setReadingTimeMinutes(5);

        TagResponse tag = new TagResponse();
        tag.setId(1L);
        tag.setName("Java");
        tag.setColor("#FF5733");
        article.setTags(List.of(tag));
        article.setImages(List.of());

        return article;
    }
}
