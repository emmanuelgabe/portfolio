package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL input type for contact form submission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactInput {

    private String name;
    private String email;
    private String subject;
    private String message;
}
