package com.emmanuelgabe.portfolio.graphql.config;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * GraphQL Configuration
 * Configures custom scalars for DateTime, Date, and Long types
 */
@Configuration
public class GraphQLConfig {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Custom DateTime scalar that handles LocalDateTime.
     * Uses ISO-8601 format (e.g., "2024-01-15T10:30:00")
     */
    private static final GraphQLScalarType LOCAL_DATE_TIME_SCALAR = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("A date-time without timezone in ISO-8601 format")
            .coercing(new Coercing<LocalDateTime, String>() {
                @Override
                public String serialize(Object dataFetcherResult,
                                         GraphQLContext graphQLContext,
                                         Locale locale) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof LocalDateTime localDateTime) {
                        return localDateTime.format(FORMATTER);
                    }
                    throw new CoercingSerializeException(
                            "Expected LocalDateTime but was " + dataFetcherResult.getClass().getName());
                }

                @Override
                public LocalDateTime parseValue(Object input,
                                                 GraphQLContext graphQLContext,
                                                 Locale locale) throws CoercingParseValueException {
                    if (input instanceof String s) {
                        try {
                            return LocalDateTime.parse(s, FORMATTER);
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseValueException("Invalid DateTime format: " + input);
                        }
                    }
                    throw new CoercingParseValueException(
                            "Expected String but was " + input.getClass().getName());
                }

                @Override
                public LocalDateTime parseLiteral(Value<?> input,
                                                   CoercedVariables variables,
                                                   GraphQLContext graphQLContext,
                                                   Locale locale) throws CoercingParseLiteralException {
                    if (input instanceof StringValue stringValue) {
                        try {
                            return LocalDateTime.parse(stringValue.getValue(), FORMATTER);
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseLiteralException("Invalid DateTime format");
                        }
                    }
                    throw new CoercingParseLiteralException("Expected StringValue");
                }
            })
            .build();

    /**
     * Configure GraphQL runtime wiring with extended scalars
     * - DateTime: ISO-8601 formatted LocalDateTime
     * - Date: ISO-8601 formatted date (LocalDate)
     * - Long: 64-bit signed integer for IDs
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(LOCAL_DATE_TIME_SCALAR)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.GraphQLLong);
    }
}
