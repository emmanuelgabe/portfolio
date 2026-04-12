package com.emmanuelgabe.portfolio.healthmonitor.tools;

import java.util.regex.Pattern;

/**
 * Defensive sanitizer for untrusted log content injected into the agent's
 * LLM context. Guards against prompt injection via log lines written by
 * visitors (e.g. manipulated request bodies echoed into server logs).
 *
 * <p>All log output returned to the agent is wrapped in an
 * {@code <untrusted-logs>} tag whose contents the system prompt explicitly
 * instructs the model to treat as data, never instructions.</p>
 */
public final class LogSanitizer {

    private static final Pattern INJECTION_PATTERNS = Pattern.compile(
            "(?i)(ignore\\s+previous\\s+instructions"
                    + "|forget\\s+all\\s+instructions"
                    + "|system\\s*:"
                    + "|assistant\\s*:"
                    + "|</?system>|</?user>|</?assistant>"
                    + "|<\\s*/\\s*untrusted-logs\\s*>)");

    private LogSanitizer() { }

    /**
     * Wrap raw log content for safe inclusion in an LLM prompt.
     * Replaces injection markers with a neutral placeholder and limits
     * the number of lines to {@code maxLines}.
     *
     * @param serviceName source container name (for the tag attribute)
     * @param rawLogs     untrusted log lines joined by newline
     * @param maxLines    hard cap on number of lines forwarded to the model
     * @return sanitized content wrapped in untrusted-logs tags
     */
    public static String sanitizeAndWrap(String serviceName, String rawLogs, int maxLines) {
        if (rawLogs == null || rawLogs.isBlank()) {
            return "<untrusted-logs source=\"" + escapeAttr(serviceName) + "\">(empty)</untrusted-logs>";
        }
        String[] lines = rawLogs.split("\\r?\\n");
        int limit = Math.min(lines.length, Math.max(maxLines, 1));
        StringBuilder sb = new StringBuilder();
        sb.append("<untrusted-logs source=\"").append(escapeAttr(serviceName)).append("\">\n");
        for (int i = lines.length - limit; i < lines.length; i++) {
            String cleaned = INJECTION_PATTERNS.matcher(lines[i]).replaceAll("[filtered]");
            sb.append(cleaned).append('\n');
        }
        sb.append("</untrusted-logs>");
        return sb.toString();
    }

    private static String escapeAttr(String value) {
        return value == null ? "" : value.replace("\"", "").replace("<", "").replace(">", "");
    }
}
