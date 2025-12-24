package com.emmanuelgabe.portfolio.graphql.resolver.mutation;

import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.graphql.input.UpdateSiteConfigurationInput;
import com.emmanuelgabe.portfolio.graphql.mapper.GraphQLInputMapper;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Mutation Resolver for Site Configuration operations.
 * All mutations require ADMIN role.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ConfigurationMutationResolver {

    private final SiteConfigurationService siteConfigurationService;
    private final GraphQLInputMapper inputMapper;

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SiteConfigurationResponse updateSiteConfiguration(@Argument UpdateSiteConfigurationInput input) {
        log.info("[GRAPHQL_MUTATION] updateSiteConfiguration");
        return siteConfigurationService.updateSiteConfiguration(
                inputMapper.toUpdateSiteConfigurationRequest(input));
    }
}
