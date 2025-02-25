package com.raul.search_service.reposiroties;

import com.raul.search_service.models.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
    @Query("""
        {
          "bool": {
            "should": [
              {
                "match": {
                  "title": {
                    "query": "?0",
                    "operator": "and"
                  }
                }
              },
              {
                "match": {
                  "content": {
                    "query": "?0",
                    "operator": "and"
                  }
                }
              },
              {
                "match": {
                  "summary": {
                    "query": "?0",
                    "operator": "and"
                  }
                }
              }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<PostDocument> search(String query, Pageable pageable);
}
