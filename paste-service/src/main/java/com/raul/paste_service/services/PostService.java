package com.raul.paste_service.services;

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

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository repository;
    private final PostConverter converter;
    private final Map<Integer, PostResponseDto> postsHash = new HashMap<>();
    private final KafkaPostProducer postProducer;

    @PostConstruct
    @Transactional
    public void init() {
        log.info("Initializing problem queue with existing problems from database.");

        var posts = repository.findAll();
        posts.forEach(post -> postsHash.put(post.getId(), converter.convertToPostResponse(post)));

        log.info("Initialization complete. {} problems added to the queue.", posts.size());
    }

    public ResponseEntity<PostResponseDto> create(PostRequestDto request) {

        log.info("Creating new post with title: {}", request.content().substring(0, 10));

        var post = converter.convertToPost(request);
        repository.save(post);
        postsHash.put(post.getId(), converter.convertToPostResponse(post));

        // TODO: send message ith kafka to hash-service
        postProducer.sendMessage(converter.convertToPostDto(post));

        log.info("Post created with ID: {}", post.getId());

        return new ResponseEntity<>(converter.convertToPostResponse(post), HttpStatus.CREATED);
    }

    public ResponseEntity<Map<Integer, PostResponseDto>> findAll() {

        log.info("Fetching all posts");

        if (postsHash.isEmpty()) {
            log.warn("No posts found");
            throw new PostNotFoundException("Sorry! I have no posts for you");
        }

        log.info("Found {} posts", postsHash.size());

        return new ResponseEntity<>(postsHash, HttpStatus.OK);
    }
}
