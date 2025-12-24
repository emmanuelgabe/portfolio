package com.emmanuelgabe.portfolio.search.repository;

import com.emmanuelgabe.portfolio.search.document.ProjectDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch repository for Project search operations.
 * Provides full-text search across title, description, techStack, and tags.
 */
@Repository
public interface ProjectSearchRepository extends ElasticsearchRepository<ProjectDocument, Long> {

    /**
     * Searches projects using multi-field query with boosting.
     * Title has highest priority (boost 3), followed by techStack and tags (boost 2),
     * then description (boost 1.5).
     *
     * @param query the search query
     * @return list of matching project documents
     */
    @Query("""
        {
            "bool": {
                "should": [
                    { "match": { "title": { "query": "?0", "boost": 3.0 } } },
                    { "match": { "techStack": { "query": "?0", "boost": 2.0 } } },
                    { "match": { "tags": { "query": "?0", "boost": 2.0 } } },
                    { "match": { "description": { "query": "?0", "boost": 1.5 } } }
                ],
                "minimum_should_match": 1
            }
        }
        """)
    List<ProjectDocument> searchByQuery(String query);
}
