package com.emmanuelgabe.portfolio.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Vector store configuration for RAG embedding storage in Elasticsearch.
 * Requires both AI and Elasticsearch to be enabled.
 *
 * <p>Creates a dedicated Elasticsearch index for vector embeddings,
 * separate from the full-text search indices managed by Spring Data ES.
 * The RestClient is created inline (not as a bean) to avoid conflicts
 * with the Spring Data Elasticsearch RestClient.</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = false)
public class VectorStoreConfig {

    @Bean
    @ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
    VectorStore vectorStore(
            EmbeddingModel embeddingModel,
            @Value("${elasticsearch.host:localhost}") String host,
            @Value("${elasticsearch.port:9200}") int port,
            @Value("${elasticsearch.username:elastic}") String username,
            @Value("${elasticsearch.password:}") String password,
            @Value("${ai.embedding.index-name:portfolio-embeddings}") String indexName,
            @Value("${ai.embedding.dimensions:768}") int dimensions) {
        log.info("[VECTOR_STORE] Initializing ElasticsearchVectorStore - host={}, port={}, index={}, dimensions={}",
                host, port, indexName, dimensions);

        RestClient restClient = buildRestClient(host, port, username, password);

        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(indexName);
        options.setDimensions(dimensions);

        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(true)
                .build();
    }

    private RestClient buildRestClient(String host, int port, String username, String password) {
        var builder = RestClient.builder(new HttpHost(host, port, "http"));

        if (password != null && !password.isEmpty()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return builder.build();
    }
}
