package com.raul.search_service.services;

import com.raul.search_service.clients.HashClient;
import com.raul.search_service.dto.PostIndexDto;
import com.raul.search_service.dto.PostResponseDto;
import com.raul.search_service.models.PostDocument;
import com.raul.search_service.reposiroties.PostSearchRepository;
import com.raul.search_service.utils.exceptions.PostNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostIndexService {
    private final PostSearchRepository repository;
    private final HashClient hashClient;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    /**
     * Listens for messages from the "post_index_topic" Kafka topic and indexes posts in Elasticsearch.
     *
     * @param postIndexDto Data transfer object containing post information to be indexed.
     */
    @KafkaListener(topics = "post_index_topic", groupId = "${spring.kafka.consumer.group-id}")
    public void addToIndex(PostIndexDto postIndexDto) {
        customLog.info(CUSTOM_LOG_MARKER, "Received post with Title: {}", postIndexDto.title());

        repository.save(convertToPostDocument(postIndexDto));

        customLog.info(CUSTOM_LOG_MARKER, "Post with Title: {} successfully added to index", postIndexDto.title());
    }

    /**
     * Searches for posts in the Elasticsearch index based on a query string.
     *
     * @param query The search query string.
     * @param page  The page number for pagination.
     * @param size  The number of results per page.
     * @return ResponseEntity containing a list of matching posts.
     */
    public ResponseEntity<List<PostResponseDto>> search(String query, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);

        List<PostDocument> searchResults = repository.search(query, pageable).toList();

        List<PostResponseDto> responseDtos = searchResults.stream()
                .map(this::convertToPostResponseDto)
                .toList();

        if (responseDtos.isEmpty()) {
            throw new PostNotFoundException("Posts not found");
        }

        return ResponseEntity.ok(responseDtos);
    }

    private PostDocument convertToPostDocument(PostIndexDto postIndexDto) {
        return PostDocument.builder()
                .id(postIndexDto.id().toString())
                .title(postIndexDto.title())
                .slug(postIndexDto.slug())
                .content(postIndexDto.content())
                .summary(postIndexDto.summary())
                .tags(postIndexDto.tags())
                .userId(postIndexDto.userId())
                .rating(postIndexDto.rating())
                .likesCount(postIndexDto.likesCount())
                .viewsCount(postIndexDto.viewsCount())
                .createdAt(postIndexDto.createdAt())
                .updatedAt(postIndexDto.updatedAt())
                .expiresAt(postIndexDto.expiresAt())
                .isDeleted(postIndexDto.isDeleted())
                .build();
    }

    private PostResponseDto convertToPostResponseDto(PostDocument postDocument) {
        return PostResponseDto.builder()
                .title(postDocument.getTitle())
                .slug(postDocument.getSlug())
                .content(postDocument.getContent())
                .summary(postDocument.getSummary())
                .tags(postDocument.getTags())
                .userId(postDocument.getUserId())
                .rating(postDocument.getRating())
                .likesCount(postDocument.getLikesCount())
                .viewsCount(postDocument.getViewsCount())
                .expirationDate(postDocument.getExpiresAt())
                .hash(hashClient.getHashByPostId(Integer.valueOf(postDocument.getId())).getBody())
                .build();
    }
}
