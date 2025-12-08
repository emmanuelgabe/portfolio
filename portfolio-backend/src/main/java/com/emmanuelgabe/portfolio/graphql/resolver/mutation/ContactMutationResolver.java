package com.emmanuelgabe.portfolio.graphql.resolver.mutation;

import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.graphql.input.ContactInput;
import com.emmanuelgabe.portfolio.graphql.mapper.GraphQLInputMapper;
import com.emmanuelgabe.portfolio.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Mutation Resolver for Contact form operations.
 * This is a PUBLIC mutation (no authentication required).
 * Rate limiting is applied at the service level.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ContactMutationResolver {

    private final ContactService contactService;
    private final GraphQLInputMapper inputMapper;

    @MutationMapping
    public ContactResponse sendContactMessage(@Argument ContactInput input) {
        log.info("[GRAPHQL_MUTATION] sendContactMessage - name={}, email={}",
                input.getName(), input.getEmail());
        return contactService.sendContactEmail(inputMapper.toContactRequest(input));
    }
}
