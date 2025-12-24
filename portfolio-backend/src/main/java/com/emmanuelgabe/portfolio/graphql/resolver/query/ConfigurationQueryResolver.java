package com.emmanuelgabe.portfolio.graphql.resolver.query;

import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.service.CvService;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Query Resolver for Site Configuration and CV operations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ConfigurationQueryResolver {

    private final SiteConfigurationService siteConfigurationService;
    private final CvService cvService;

    @QueryMapping
    public SiteConfigurationResponse siteConfiguration() {
        log.debug("[GRAPHQL_QUERY] siteConfiguration");
        return siteConfigurationService.getSiteConfiguration();
    }

    @QueryMapping
    public CvResponse currentCv() {
        log.debug("[GRAPHQL_QUERY] currentCv");
        return cvService.getCurrentCv().orElse(null);
    }
}
