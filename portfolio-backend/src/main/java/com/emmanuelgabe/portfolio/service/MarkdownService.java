package com.emmanuelgabe.portfolio.service;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Service for rendering Markdown content to HTML using Flexmark library.
 * Supports GitHub Flavored Markdown extensions: tables, strikethrough, task lists, and autolinks.
 */
@Service
@Slf4j
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownService() {
        MutableDataSet options = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create()
            ))
            .set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
        this.safelist = Safelist.relaxed()
            .addTags("input", "del")
            .addAttributes("input", "type", "checked", "disabled")
            .addAttributes("code", "class")
            .addAttributes("pre", "class")
            .addAttributes(":all", "class");
    }

    /**
     * Renders Markdown content to HTML.
     *
     * @param markdown the Markdown source
     * @return the rendered HTML
     */
    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        try {
            Node document = parser.parse(markdown);
            String html = renderer.render(document);
            return Jsoup.clean(html, safelist);
        } catch (Exception e) {
            log.error("[MARKDOWN_RENDER] Failed to render Markdown - error={}", e.getMessage(), e);
            return "<p>Error rendering content</p>";
        }
    }

    /**
     * Extracts a plain text excerpt from Markdown content.
     * Strips HTML tags and truncates to specified length.
     *
     * @param markdown the Markdown source
     * @param maxLength maximum length of the excerpt
     * @return the plain text excerpt
     */
    public String extractExcerpt(String markdown, int maxLength) {
        String html = renderToHtml(markdown);
        String text = html.replaceAll("<[^>]*>", "");
        text = text.trim().replaceAll("\\s+", " ");

        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim() + "...";
    }
}
