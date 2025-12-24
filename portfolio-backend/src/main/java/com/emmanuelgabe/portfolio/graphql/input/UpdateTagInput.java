package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL input type for updating a tag.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTagInput {

    private String name;
    private String color;
}
