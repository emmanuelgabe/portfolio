package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input type for offset-based pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageInput {

    private Integer page = 0;
    private Integer size = 10;

    public int getPageOrDefault() {
        return page != null ? page : 0;
    }

    public int getSizeOrDefault() {
        return size != null ? Math.min(size, 100) : 10;
    }
}
