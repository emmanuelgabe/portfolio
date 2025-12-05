package com.emmanuelgabe.portfolio.graphql.util;

import com.emmanuelgabe.portfolio.graphql.input.PageInput;
import com.emmanuelgabe.portfolio.graphql.type.connection.Connection;
import com.emmanuelgabe.portfolio.graphql.type.connection.Edge;
import com.emmanuelgabe.portfolio.graphql.type.connection.PageInfo;
import org.springframework.data.domain.Page;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Utility class for building Relay-style Connection objects.
 */
public final class ConnectionUtil {

    private static final String CURSOR_PREFIX = "cursor:";

    private ConnectionUtil() {
        // Utility class
    }

    /**
     * Convert a Spring Data Page to a Relay Connection.
     *
     * @param page the Spring Data page
     * @param idExtractor function to extract ID from entity for cursor generation
     * @param <T> the entity type
     * @return Relay Connection
     */
    public static <T> Connection<T> fromPage(Page<T> page, Function<T, Long> idExtractor) {
        List<T> content = page.getContent();

        List<Edge<T>> edges = IntStream.range(0, content.size())
                .mapToObj(i -> {
                    T item = content.get(i);
                    String cursor = encodeCursor(idExtractor.apply(item));
                    return new Edge<>(item, cursor);
                })
                .toList();

        PageInfo pageInfo = PageInfo.builder()
                .hasNextPage(page.hasNext())
                .hasPreviousPage(page.hasPrevious())
                .startCursor(edges.isEmpty() ? null : edges.get(0).getCursor())
                .endCursor(edges.isEmpty() ? null : edges.get(edges.size() - 1).getCursor())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return Connection.<T>builder()
                .edges(edges)
                .pageInfo(pageInfo)
                .totalCount(page.getTotalElements())
                .build();
    }

    /**
     * Convert a list to a Relay Connection with manual pagination.
     *
     * @param items the full list of items
     * @param pageInput pagination input
     * @param idExtractor function to extract ID from entity for cursor generation
     * @param <T> the entity type
     * @return Relay Connection
     */
    public static <T> Connection<T> fromList(List<T> items, PageInput pageInput, Function<T, Long> idExtractor) {
        int page = pageInput != null ? pageInput.getPageOrDefault() : 0;
        int size = pageInput != null ? pageInput.getSizeOrDefault() : 10;

        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<T> pageContent = start < totalElements ? items.subList(start, end) : List.of();

        List<Edge<T>> edges = IntStream.range(0, pageContent.size())
                .mapToObj(i -> {
                    T item = pageContent.get(i);
                    String cursor = encodeCursor(idExtractor.apply(item));
                    return new Edge<>(item, cursor);
                })
                .toList();

        PageInfo pageInfo = PageInfo.builder()
                .hasNextPage(end < totalElements)
                .hasPreviousPage(page > 0)
                .startCursor(edges.isEmpty() ? null : edges.get(0).getCursor())
                .endCursor(edges.isEmpty() ? null : edges.get(edges.size() - 1).getCursor())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();

        return Connection.<T>builder()
                .edges(edges)
                .pageInfo(pageInfo)
                .totalCount(totalElements)
                .build();
    }

    /**
     * Encode an ID to a cursor string.
     */
    public static String encodeCursor(Long id) {
        String raw = CURSOR_PREFIX + id;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode a cursor string to an ID.
     */
    public static Long decodeCursor(String cursor) {
        String raw = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
        if (raw.startsWith(CURSOR_PREFIX)) {
            return Long.parseLong(raw.substring(CURSOR_PREFIX.length()));
        }
        throw new IllegalArgumentException("Invalid cursor format");
    }
}
