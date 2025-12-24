package com.emmanuelgabe.portfolio.graphql.type.connection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic Edge type for Relay Connection pagination.
 *
 * @param <T> the node type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Edge<T> {

    private T node;
    private String cursor;
}
