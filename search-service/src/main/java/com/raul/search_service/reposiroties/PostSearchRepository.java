package com.raul.search_service.reposiroties;

import com.raul.search_service.models.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
    @Query("{\"bool\": {\"should\": [" +
            "{\"match\": {\"title\": \"?0\"}}," +
            "{\"match\": {\"content\": \"?0\"}}," +
            "{\"match\": {\"summary\": \"?0\"}}" +
            "]}}")
    Page<PostDocument> search(String query, Pageable pageable);
}
