package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL input type for creating a tag.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTagInput {

    private String name;
    private String color;
}
