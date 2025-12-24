package com.emmanuelgabe.portfolio.search.repository;

import com.emmanuelgabe.portfolio.search.document.ExperienceDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch repository for Experience search operations.
 * Provides full-text search across company, role, and description.
 */
@Repository
public interface ExperienceSearchRepository extends ElasticsearchRepository<ExperienceDocument, Long> {

    /**
     * Searches experiences using multi-field query with boosting.
     * Company and role have highest priority (boost 3),
     * description has lower priority (boost 1).
     *
     * @param query the search query
     * @return list of matching experience documents
     */
    @Query("""
        {
            "bool": {
                "should": [
                    { "match": { "company": { "query": "?0", "boost": 3.0 } } },
                    { "match": { "role": { "query": "?0", "boost": 3.0 } } },
                    { "match": { "description": { "query": "?0", "boost": 1.0 } } }
                ],
                "minimum_should_match": 1
            }
        }
        """)
    List<ExperienceDocument> searchByQuery(String query);
}
