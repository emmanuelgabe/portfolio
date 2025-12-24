package com.emmanuelgabe.portfolio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownServiceTest {

    private MarkdownService markdownService;

    @BeforeEach
    void setUp() {
        markdownService = new MarkdownService();
    }

    @Test
    void should_renderBasicMarkdownToHtml_when_validMarkdownProvided() {
        String markdown = "# Heading\n\nThis is a paragraph.";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<h1>Heading</h1>");
        assertThat(result).contains("<p>This is a paragraph.</p>");
    }

    @Test
    void should_renderEmptyString_when_nullMarkdownProvided() {
        String result = markdownService.renderToHtml(null);

        assertThat(result).isEmpty();
    }

    @Test
    void should_renderEmptyString_when_blankMarkdownProvided() {
        String result = markdownService.renderToHtml("   ");

        assertThat(result).isEmpty();
    }

    @Test
    void should_renderTableMarkdown_when_tableProvided() {
        String markdown = "| Header 1 | Header 2 |\n|----------|----------|\n| Cell 1   | Cell 2   |";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<table>");
        assertThat(result).contains("<th>Header 1</th>");
        assertThat(result).contains("<td>Cell 1</td>");
    }

    @Test
    void should_renderStrikethroughMarkdown_when_strikethroughProvided() {
        String markdown = "~~strikethrough~~";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<del>strikethrough</del>");
    }

    @Test
    void should_renderTaskListMarkdown_when_taskListProvided() {
        String markdown = "- [ ] Unchecked task\n- [x] Checked task";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<input");
        assertThat(result).contains("type=\"checkbox\"");
    }

    @Test
    void should_renderAutolink_when_urlProvided() {
        String markdown = "Visit https://example.com";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("href=\"https://example.com\"");
    }

    @Test
    void should_renderCodeBlock_when_codeBlockProvided() {
        String markdown = "```java\npublic class Test {}\n```";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<code");
        assertThat(result).contains("public class Test");
    }

    @Test
    void should_renderErrorMessage_when_invalidMarkdownProvided() {
        String invalidMarkdown = null;

        String result = markdownService.renderToHtml(invalidMarkdown);

        assertThat(result).isEmpty();
    }

    @Test
    void should_extractExcerpt_when_markdownWithMultipleParagraphs() {
        String markdown = "This is the first paragraph.\n\nThis is the second paragraph.";

        String result = markdownService.extractExcerpt(markdown, 20);

        assertThat(result).hasSize(23);
        assertThat(result).endsWith("...");
        assertThat(result).doesNotContain("<p>");
    }

    @Test
    void should_returnFullText_when_excerptLengthExceedsContent() {
        String markdown = "Short text";

        String result = markdownService.extractExcerpt(markdown, 100);

        assertThat(result).isEqualTo("Short text");
        assertThat(result).doesNotEndWith("...");
    }

    @Test
    void should_stripHtmlTags_when_extractExcerpt() {
        String markdown = "**Bold text** and *italic text*";

        String result = markdownService.extractExcerpt(markdown, 50);

        assertThat(result).doesNotContain("<strong>");
        assertThat(result).doesNotContain("<em>");
        assertThat(result).contains("Bold text");
        assertThat(result).contains("italic text");
    }

    @Test
    void should_normalizeWhitespace_when_extractExcerpt() {
        String markdown = "Text   with    multiple    spaces";

        String result = markdownService.extractExcerpt(markdown, 50);

        assertThat(result).doesNotContain("   ");
        assertThat(result).isEqualTo("Text with multiple spaces");
    }

    @Test
    void should_renderBoldAndItalic_when_emphasisMarkdownProvided() {
        String markdown = "**bold** and *italic*";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<strong>bold</strong>");
        assertThat(result).contains("<em>italic</em>");
    }

    @Test
    void should_renderListMarkdown_when_listProvided() {
        String markdown = "- Item 1\n- Item 2\n- Item 3";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<ul>");
        assertThat(result).contains("<li>Item 1</li>");
        assertThat(result).contains("<li>Item 2</li>");
    }

    @Test
    void should_renderOrderedList_when_orderedListProvided() {
        String markdown = "1. First\n2. Second\n3. Third";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<ol>");
        assertThat(result).contains("<li>First</li>");
    }

    @Test
    void should_renderComplexMarkdown_when_mixedContentProvided() {
        String markdown = "# Title\n\n**Bold** text with [link](https://example.com)\n\n- List item\n\n```code```";

        String result = markdownService.renderToHtml(markdown);

        assertThat(result).contains("<h1>Title</h1>");
        assertThat(result).contains("<strong>Bold</strong>");
        assertThat(result).contains("href=\"https://example.com\"");
        assertThat(result).contains("<ul>");
    }

    // ========== XSS Protection Tests ==========

    @Test
    void should_stripScriptTags_when_xssAttemptProvided() {
        // Arrange
        String markdown = "Normal text <script>alert('XSS')</script> more text";

        // Act
        String result = markdownService.renderToHtml(markdown);

        // Assert
        assertThat(result).doesNotContain("<script>");
        assertThat(result).doesNotContain("alert");
        assertThat(result).contains("Normal text");
        assertThat(result).contains("more text");
    }

    @Test
    void should_stripOnEventHandlers_when_xssAttemptProvided() {
        // Arrange
        String markdown = "<img src=x onerror=\"alert('XSS')\">";

        // Act
        String result = markdownService.renderToHtml(markdown);

        // Assert
        assertThat(result).doesNotContain("onerror");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void should_stripJavascriptUrls_when_xssAttemptProvided() {
        // Arrange
        String markdown = "[Click me](javascript:alert('XSS'))";

        // Act
        String result = markdownService.renderToHtml(markdown);

        // Assert
        assertThat(result).doesNotContain("javascript:");
    }
}
