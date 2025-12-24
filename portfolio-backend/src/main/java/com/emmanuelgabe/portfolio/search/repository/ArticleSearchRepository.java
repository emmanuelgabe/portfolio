package com.emmanuelgabe.portfolio.search.repository;

import com.emmanuelgabe.portfolio.search.document.ArticleDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch repository for Article search operations.
 * Provides full-text search across title, content, excerpt, and tags.
 */
@Repository
public interface ArticleSearchRepository extends ElasticsearchRepository<ArticleDocument, Long> {

    /**
     * Searches articles using multi-field query with boosting.
     * Title has highest priority (boost 3), followed by excerpt and tags (boost 2),
     * then content (boost 1).
     *
     * @param query the search query
     * @return list of matching article documents
     */
    @Query("""
        {
            "bool": {
                "should": [
                    { "match": { "title": { "query": "?0", "boost": 3.0 } } },
                    { "match": { "excerpt": { "query": "?0", "boost": 2.0 } } },
                    { "match": { "tags": { "query": "?0", "boost": 2.0 } } },
                    { "match": { "content": { "query": "?0", "boost": 1.0 } } }
                ],
                "minimum_should_match": 1
            }
        }
        """)
    List<ArticleDocument> searchByQuery(String query);
}
