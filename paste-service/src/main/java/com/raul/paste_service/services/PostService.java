package com.raul.paste_service.services;

import com.raul.paste_service.clients.HashClient;
import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.utils.exceptions.PostNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository repository;
    private final PostConverter converter;
    private final Map<Integer, PostResponseDto> posts = new HashMap<>();
    private final KafkaPostProducer postProducer;
    private final HashClient hashClient;

    @PostConstruct
    @Transactional
    public void init() {
        log.info("Initializing post list with existing posts from database.");

        try {
            List<Object[]> postsList = repository.findAllPostsWithHashes();
            log.info("Found {} posts in the database.", postsList.size());

            posts.putAll(postsList.stream().collect(Collectors.toMap(
                    row -> (Integer) row[0], // postId
                    row -> new PostResponseDto(
                            (String) row[1], // content
                            (Integer) row[2], // userId
                            ((Instant) row[3]).atZone(ZoneId.systemDefault()).toLocalDateTime(), // expirationDate
                            (String) row[4]  // hash
                    )
            )));

        } catch (Exception e) {
            log.error("Error initializing posts from database", e);
        }
        log.info("Initialization complete. {} posts added to the map.", posts.size());
    }

    public ResponseEntity<PostResponseDto> create(PostRequestDto request) throws InterruptedException {

        log.info("Creating new post with title: {}", request.content().substring(0, 10));

        var post = converter.convertToPost(request);
        repository.save(post);

        postProducer.sendMessage(converter.convertToPostDto(post));

        Thread.sleep(1000);

        PostResponseDto postResponse = converter.convertToPostResponse(post);

        posts.put(post.getId(), postResponse);

        log.info("Post created with ID: {}", post.getId());

        return new ResponseEntity<>(postResponse, HttpStatus.CREATED);
    }

    public ResponseEntity<Map<Integer, PostResponseDto>> findAll() {

        log.info("Fetching all posts");

        if (posts.isEmpty()) {
            log.warn("No posts found");
            throw new PostNotFoundException("Sorry! I have no posts for you");
        }

        log.info("Found {} posts", posts.size());

        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    public ResponseEntity<PostResponseDto> getPostByHash(String hash) {
        log.info("Received request to find post by hash: {}", hash);

        Integer postId;
        try {
            postId = hashClient.getPostIdByHash(hash).getBody();
        } catch (Exception e) {
            log.error("Error occurred while calling hashClient for hash: {}", hash);
            throw new RuntimeException("Error retrieving post ID by hash", e);
        }

        if (postId == null) {
            log.warn("Post ID is null for hash: {}", hash);
            throw new PostNotFoundException("Post not found");
        }

        log.info("Post ID found: {}", postId);

        PostResponseDto post = posts.get(postId);
        if (post == null) {
            log.warn("No post found for Post ID: {}", postId);
            throw new PostNotFoundException("Post not found");
        }

        log.info("Returning post for Post ID: {}", postId);

        return new ResponseEntity<>(post, HttpStatus.OK);
    }
}
