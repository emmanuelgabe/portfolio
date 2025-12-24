package com.emmanuelgabe.portfolio.graphql.type.connection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic Connection type for Relay pagination.
 *
 * @param <T> the node type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Connection<T> {

    private List<Edge<T>> edges;
    private PageInfo pageInfo;
    private long totalCount;
}
