package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioSearchToolTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ToolCallTracker tracker;

    @InjectMocks
    private PortfolioSearchTool portfolioSearchTool;

    // ========== searchPortfolio ==========

    @Test
    void should_returnFormattedResults_when_documentsFound() {
        // Arrange
        Document doc = new Document("doc-1", "Built a microservices platform",
                Map.of("type", "project", "title", "Portfolio Backend"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc));

        // Act
        String result = portfolioSearchTool.searchPortfolio("microservices");

        // Assert
        assertThat(result).contains("--- Result 1 ---");
        assertThat(result).contains("Type: project");
        assertThat(result).contains("Title: Portfolio Backend");
        assertThat(result).contains("Built a microservices platform");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void should_returnMultipleResults_when_multipleDocumentsFound() {
        // Arrange
        Document doc1 = new Document("doc-1", "First document",
                Map.of("type", "project", "title", "Project A"));
        Document doc2 = new Document("doc-2", "Second document",
                Map.of("type", "article", "title", "Article B"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc1, doc2));

        // Act
        String result = portfolioSearchTool.searchPortfolio("test query");

        // Assert
        assertThat(result).contains("--- Result 1 ---");
        assertThat(result).contains("--- Result 2 ---");
        assertThat(result).contains("Project A");
        assertThat(result).contains("Article B");
    }

    @Test
    void should_returnNoResultsMessage_when_noDocumentsFound() {
        // Arrange
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        // Act
        String result = portfolioSearchTool.searchPortfolio("nonexistent topic");

        // Assert
        assertThat(result).contains("No relevant portfolio content found");
        assertThat(result).contains("nonexistent topic");
    }

    @Test
    void should_returnUnavailableMessage_when_vectorSearchFails() {
        // Arrange
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Elasticsearch down"));

        // Act
        String result = portfolioSearchTool.searchPortfolio("any query");

        // Assert
        assertThat(result).contains("temporarily unavailable");
    }

    @Test
    void should_truncateLongContent_when_documentContentExceedsLimit() {
        // Arrange
        String longContent = "x".repeat(1000);
        Document doc = new Document("doc-1", longContent,
                Map.of("type", "article", "title", "Long Article"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc));

        // Act
        String result = portfolioSearchTool.searchPortfolio("query");

        // Assert
        assertThat(result).contains("...");
        assertThat(result).doesNotContain("x".repeat(1000));
    }

    @Test
    void should_handleMissingMetadata_when_documentHasNoTypeOrTitle() {
        // Arrange
        Document doc = new Document("Some content");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc));

        // Act
        String result = portfolioSearchTool.searchPortfolio("query");

        // Assert
        assertThat(result).contains("--- Result 1 ---");
        assertThat(result).contains("Some content");
    }
}
