package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

/**
 * Agent tool for semantic search across the entire portfolio knowledge base.
 * Uses the Elasticsearch vector store with embedding similarity to find
 * relevant content for broad or cross-cutting questions.
 */
@Slf4j
@RequiredArgsConstructor
public class PortfolioSearchTool {

    private static final int MAX_DOCUMENTS = 5;
    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final int MAX_CONTENT_LENGTH = 600;

    private final VectorStore vectorStore;
    private final ToolCallTracker tracker;

    /**
     * Semantic search across all indexed portfolio content.
     *
     * @param query natural language search query
     * @return formatted text with matching document excerpts, or a no-results message
     */
    @Tool(description = "Search the portfolio knowledge base using semantic similarity. "
            + "Use for broad, general, or cross-cutting questions about Emmanuel's background, "
            + "philosophy, approach, or when other specific tools do not match the question.")
    public String searchPortfolio(
            @ToolParam(description = "Natural language search query") String query) {
        tracker.record("searchPortfolio");
        log.debug("[CHAT_TOOL] searchPortfolio - query={}", query);
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(MAX_DOCUMENTS)
                            .similarityThreshold(SIMILARITY_THRESHOLD)
                            .build()
            );
            if (docs.isEmpty()) {
                return "No relevant portfolio content found for: " + query;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < docs.size(); i++) {
                Document doc = docs.get(i);
                Map<String, Object> metadata = doc.getMetadata();
                sb.append("--- Result ").append(i + 1).append(" ---\n");
                sb.append("Type: ").append(getMetadata(metadata, "type")).append("\n");
                sb.append("Title: ").append(getMetadata(metadata, "title")).append("\n");
                sb.append("Content: ").append(truncate(doc.getText())).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[CHAT_TOOL] searchPortfolio failed - query={}, error={}", query, e.getMessage());
            return "Portfolio search is temporarily unavailable.";
        }
    }

    private static String getMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return "";
        }
        Object value = metadata.get(key);
        return value != null ? value.toString() : "";
    }

    private static String truncate(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_LENGTH) + "...";
    }
}
