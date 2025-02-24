package com.raul.paste_service.services.postServices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raul.paste_service.clients.HashClient;
import com.raul.paste_service.dto.PostIdDto;
import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.dto.TagResponseDto;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.Tag;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.TagRepository;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import com.raul.paste_service.utils.exceptions.PostNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final PostConverter converter;
    private final HashClient hashClient;
    private final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
    private final ObjectMapper mapper;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");
    private final TagRepository tagRepository;
    private final KafkaProducer kafkaProducer;

    @Transactional
    public ResponseEntity<PostResponseDto> create(PostRequestDto request) throws InterruptedException {

        customLog.info(CUSTOM_LOG_MARKER, "Creating new post");

        Post post;
        String slug = generateUniqueSlug(request.title());
        post = converter.convertToPost(request, slug);

        postRepository.save(post);

        savePostTags(post, request.tags());

        hashClient.generateHash(new PostIdDto(post.getId()));

        PostResponseDto postResponse = converter.convertToPostResponse(post);
        postResponse.setTags(request.tags().stream()
                                    .map(TagResponseDto::new)
                                    .collect(Collectors.toList()));

        kafkaProducer.sendMessageToPostIndexTopic(converter.convertToPostIndex(post));

        customLog.info(CUSTOM_LOG_MARKER, "Post created with ID: {}", post.getId());

        return new ResponseEntity<>(postResponse, HttpStatus.CREATED);
    }

    public ResponseEntity<PostResponseDto> getPostByHash(String hash) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to find post by hash: {}", hash);

        Integer postId;
        try {
            postId = hashClient.getPostIdByHash(hash).getBody();

            if (postId == null) {
                customLog.warn(CUSTOM_LOG_MARKER, "Post ID is null for hash: {}", hash);
                throw new PostNotFoundException("Post not found");
            }
        } catch (Exception e) {
            customLog.error(CUSTOM_LOG_MARKER, "Error occurred while calling hashClient for hash: {}", hash);
            throw new RuntimeException("Error retrieving post ID by hash", e);
        }

        customLog.info(CUSTOM_LOG_MARKER, "Post ID found: {}", postId);

        postRepository.incrementViews(postId);

        PostResponseDto post;
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "post:%d".formatted(postId);
            String raw = jedis.get(key);
            if (raw != null) {
                customLog.info(CUSTOM_LOG_MARKER, "Post found in Redis");

                post = mapper.readValue(raw, PostResponseDto.class);

                customLog.info(CUSTOM_LOG_MARKER, "Returning post for Post ID: {}", postId);
                return new ResponseEntity<>(post, HttpStatus.OK);
            }

            post = getPostById(postId);
            if (post == null) {
                customLog.warn(CUSTOM_LOG_MARKER, "No post found for Post ID: {}", postId);
                throw new PostNotFoundException("Post not found");
            }

            customLog.info(CUSTOM_LOG_MARKER, "Returning post for Post ID: {}", postId);

            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private PostResponseDto getPostById(Integer id) {
        return converter.convertToPostResponse(Objects.requireNonNull(postRepository.findById(id).orElse(null)));
    }

    @Transactional
    public ResponseEntity<PostResponseDto> addLike(Integer postId) {
        postRepository.incrementLikes(postId);

        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        customLog.info(CUSTOM_LOG_MARKER, "Like added to post with ID: {}", postId);

        return new ResponseEntity<>(converter.convertToPostResponse(post), HttpStatus.OK);
    }

    private String generateUniqueSlug(String title) {

        String suffix = UUID.randomUUID().toString();

        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");

        int maxSlugLength = 100 - suffix.length() - 1;
        if (baseSlug.length() > maxSlugLength) {
            baseSlug = baseSlug.substring(0, maxSlugLength);
        }

        return baseSlug + "-" + suffix;
    }

    private void savePostTags(Post post, List<String> tagNames) {
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

            post.getTags().add(tag);
        }
        postRepository.save(post);
    }
}
