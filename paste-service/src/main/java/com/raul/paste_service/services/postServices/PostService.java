package com.raul.paste_service.services.postServices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raul.paste_service.clients.HashClient;
import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import com.raul.paste_service.utils.exceptions.PostNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Objects;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final PostConverter converter;
    private final KafkaProducer postProducer;
    private final HashClient hashClient;
    private final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
    private final ObjectMapper mapper;

    public ResponseEntity<PostResponseDto> create(PostRequestDto request) throws InterruptedException {

        log.info("Creating new post");

        var post = converter.convertToPost(request);
        postRepository.save(post);

        postProducer.sendMessageToHashTopic(converter.convertToPostDto(post));

        Thread.sleep(500);

        PostResponseDto postResponse = converter.convertToPostResponse(post);

        log.info("Post created with ID: {}", post.getId());

        return new ResponseEntity<>(postResponse, HttpStatus.CREATED);
    }

    public ResponseEntity<PostResponseDto> getPostByHash(String hash) {
        log.info("Received request to find post by hash: {}", hash);

        Integer postId;
        try {
            postId = hashClient.getPostIdByHash(hash).getBody();

            if (postId == null) {
                log.warn("Post ID is null for hash: {}", hash);
                throw new PostNotFoundException("Post not found");
            }
        } catch (Exception e) {
            log.error("Error occurred while calling hashClient for hash: {}", hash);
            throw new RuntimeException("Error retrieving post ID by hash", e);
        }

        log.info("Post ID found: {}", postId);

        postRepository.incrementViews(postId);

        PostResponseDto post;
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "post:%d".formatted(postId);
            String raw = jedis.get(key);
            if (raw != null) {
                log.info("Post found in Redis");

                post = mapper.readValue(raw, PostResponseDto.class);

                log.info("Returning post for Post ID: {}", postId);
                return new ResponseEntity<>(post, HttpStatus.OK);
            }

            post = getPostById(postId);
            if (post == null) {
                log.warn("No post found for Post ID: {}", postId);
                throw new PostNotFoundException("Post not found");
            }

            log.info("Returning post for Post ID: {}", postId);

            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private PostResponseDto getPostById(Integer id) {
        return converter.convertToPostResponse(Objects.requireNonNull(postRepository.findById(id).orElse(null)));
    }

    public ResponseEntity<PostResponseDto> getRandomPost() {
        long count = postRepository.count();

        if (count == 0) {
            throw new PostNotFoundException("Posts not found");
        }

        long randomPostId = new Random().nextLong(1, count);

        postRepository.incrementViews((int) randomPostId);

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "post:%d".formatted(randomPostId);
            String raw = jedis.get(key);
            if (raw != null) {
                log.info("Post found in Redis");

                return new ResponseEntity<>(
                        mapper.readValue(raw, PostResponseDto.class),
                        HttpStatus.OK
                );
            }

            var post = getPostById((int) randomPostId);

            log.info("Returning post for Post ID: {}", randomPostId);
            return new ResponseEntity<>(post, HttpStatus.OK);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public ResponseEntity<PostResponseDto> addLike(Integer postId) {
        postRepository.incrementLikes(postId);

        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        log.info("Like added to post with ID: {}", postId);

        return new ResponseEntity<>(converter.convertToPostResponse(post), HttpStatus.OK);
    }
}
