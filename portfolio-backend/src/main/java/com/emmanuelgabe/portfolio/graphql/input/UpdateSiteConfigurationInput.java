package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL input type for updating site configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSiteConfigurationInput {

    private String fullName;
    private String email;
    private String heroTitle;
    private String heroDescription;
    private String siteTitle;
    private String seoDescription;
    private String githubUrl;
    private String linkedinUrl;
}
