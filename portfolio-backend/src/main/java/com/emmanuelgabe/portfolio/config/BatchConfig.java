package com.emmanuelgabe.portfolio.config;

import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * Spring Batch configuration for scheduled jobs.
 * Includes audit log cleanup and report generation jobs.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class BatchConfig {

    /**
     * Initialize Spring Batch schema on PostgreSQL.
     * Uses the default Spring Batch DDL scripts.
     */
    @Bean
    BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(
            DataSource dataSource, BatchProperties properties) {
        return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
    }
}
