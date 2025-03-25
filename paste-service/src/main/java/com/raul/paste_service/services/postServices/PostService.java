package com.raul.paste_service.services.postServices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raul.paste_service.clients.HashClient;
import com.raul.paste_service.dto.*;
import com.raul.paste_service.dto.post.PostIdDto;
import com.raul.paste_service.dto.post.PostRequestDto;
import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.Tag;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.TagRepository;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import com.raul.paste_service.services.schedulerServices.PostCleanUpService;
import com.raul.paste_service.utils.exceptions.PostNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostConverter converter;
    private final HashClient hashClient;
    private final JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
    private final ObjectMapper mapper;
    private static final Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");
    private final TagRepository tagRepository;
    private final KafkaProducer kafkaProducer;
    private final PostCleanUpService postCleanUpService;

    /**
     * Creates a new post and saves it to the database.
     *
     * @param request Post request DTO.
     * @return ResponseEntity with created PostResponseDto.
     */
    @Transactional
    public ResponseEntity<PostResponseDto> create(PostRequestDto request) {
        customLog.info(CUSTOM_LOG_MARKER, "Creating new post");

        Post post = converter.convertToPost(request);

        postRepository.save(post);

        customLog.info(CUSTOM_LOG_MARKER, "Saving tags");
        savePostTags(post, request.tags());

        String hash;
        try {
            customLog.info(CUSTOM_LOG_MARKER, "Generating unique hash");
            hash = hashClient.generateHash(new PostIdDto(post.getId())).getBody();
        } catch (Exception e) {
            customLog.error(CUSTOM_LOG_MARKER, "Failed to create a hash for post", e);
            throw new RuntimeException("Failed to create a post. Please try later");
        }

        PostResponseDto postResponse = converter.convertToPostResponse(post);
        postResponse.setHash(hash);

        postResponse.setTags(request.tags().stream()
                                    .map(TagResponseDto::new)
                                    .collect(Collectors.toList()));

        customLog.info(CUSTOM_LOG_MARKER, "Sending to search-service");
        kafkaProducer.sendMessageToPostIndexTopic(converter.convertToPostIndex(post));

        customLog.info(CUSTOM_LOG_MARKER, "Post created with ID: {}", post.getId());
        return new ResponseEntity<>(postResponse, HttpStatus.CREATED);
    }

    /**
     * Retrieves a post by its hash.
     *
     * @param hash Hash to search for.
     * @return ResponseEntity with the found PostResponseDto.
     */
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
            customLog.error(CUSTOM_LOG_MARKER, "Error occurred while calling hashClient for hash: {}", hash, e);
            throw new PostNotFoundException("Post not found");
        }

        customLog.info(CUSTOM_LOG_MARKER, "Post ID found: {}", postId);

        postRepository.incrementViews(postId);

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "post:%d".formatted(postId);
            String raw = jedis.get(key);
            if (raw != null) {
                customLog.info(CUSTOM_LOG_MARKER, "Post found in Redis");

                PostResponseDto postResponseDto = mapper.readValue(raw, PostResponseDto.class);
                postResponseDto.setHash(hash);
                return new ResponseEntity<>(postResponseDto, HttpStatus.OK);
            }

            var post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException("Post not found"));

            PostResponseDto postResponseDto = converter.convertToPostResponse(post);
            postResponseDto.setHash(hash);
//            postResponseDto.setTags(post.getTags().stream()
//                    .map(tag -> new TagResponseDto(tag.getName()))
//                    .collect(Collectors.toList()));

            jedis.setex(key, 3600, mapper.writeValueAsString(postResponseDto));


            return new ResponseEntity<>(postResponseDto, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            customLog.error("Error occurred while saving postResponseDto to redis", e);
            throw new RuntimeException("Something went wrong. Please, try later");
        }
    }

    /**
     * Retrieves a post by its slug.
     *
     * @param slug Slug to search for.
     * @return ResponseEntity with the found PostResponseDto.
     */
    public ResponseEntity<PostResponseDto> getPostBySlug(String slug) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to find post by slug: {}", slug);

        var post = postRepository.findPostBySlug(slug)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        PostResponseDto postResponseDto = converter.convertToPostResponse(post);

        postRepository.incrementViews(post.getId());

        postResponseDto.setHash(hashClient.getHashByPostId(post.getId()).getBody());
//        postResponseDto.setTags(post.getTags().stream()
//                .map(tag -> new TagResponseDto(tag.getName()))
//                .collect(Collectors.toList()));

        return new ResponseEntity<>(postResponseDto, HttpStatus.OK);
    }

    /**
     * Deletes a single post by its ID.
     *
     * @param postId The ID of the post to delete.
     * @return ResponseEntity with status NO_CONTENT if deletion is successful, CONFLICT if post is already deleted.
     */
    @Transactional
    public ResponseEntity<Void> deletePost(Integer postId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to delete post by post ID: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        if (post.getIsDeleted()) {
            customLog.warn(CUSTOM_LOG_MARKER, "Post with ID: {} is already deleted", postId);
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        post.setIsDeleted(true);
        post.setExpiresAt(LocalDateTime.now());
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);

        customLog.info(CUSTOM_LOG_MARKER, "Sending to search-service for updating in posts index");
        kafkaProducer.sendMessageToPostIndexTopic(converter.convertToPostIndex(post));

        customLog.info(CUSTOM_LOG_MARKER, "Post with ID: {} marked as deleted", postId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Deletes all posts by a user based on the user ID.
     *
     * @param userId The ID of the user whose posts will be deleted.
     * @return ResponseEntity with status NO_CONTENT if deletion is successful, NOT_FOUND if no posts are found.
     */
    @Transactional
    public ResponseEntity<Void> deleteAllPostByUserId(Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to delete posts by user ID: {}", userId);

        List<Post> posts = postRepository.findAllByUserId(userId);
        if (posts.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No posts found for user ID: {}", userId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        customLog.info(CUSTOM_LOG_MARKER, "Sending request to hash-service for mark hash as deleted");
        postCleanUpService.sendDeleteRequestToHashService(posts);

        posts.forEach(post -> {
            post.setIsDeleted(true);
            post.setDeletedAt(LocalDateTime.now());
        });

        postRepository.saveAll(posts);

        customLog.info(CUSTOM_LOG_MARKER, "Sending to search-service for updating in posts index");
        posts.stream()
                .map(converter::convertToPostIndex)
                .forEach(kafkaProducer::sendMessageToPostIndexTopic);

        customLog.info(CUSTOM_LOG_MARKER, "Posts with user ID: {} marked as deleted", userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Adds a like to the post by its ID.
     *
     * @param postId ID of the post to like.
     * @return ResponseEntity with updated PostResponseDto containing the like count.
     */
    @Transactional
    public ResponseEntity<PostResponseDto> addLike(Integer postId) {
        postRepository.incrementLikes(postId);

        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        PostResponseDto postResponseDto = converter.convertToPostResponse(post);
        postResponseDto.setHash(hashClient.getHashByPostId(postId).getBody());
//        postResponseDto.setTags(post.getTags().stream()
//                .map(tag -> new TagResponseDto(tag.getName()))
//                .collect(Collectors.toList()));

        customLog.info(CUSTOM_LOG_MARKER, "Like added to post with ID: {}", postId);

        return new ResponseEntity<>(postResponseDto, HttpStatus.OK);
    }

    /**
     * Saves tags to the post.
     * If the tag does not exist, it will be created and added to the post.
     *
     * @param post The post to which the tags will be saved.
     * @param tagNames List of tag names to associate with the post.
     */
    private void savePostTags(Post post, List<String> tagNames) {
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

            post.getTags().add(tag);
        }
        postRepository.save(post);
    }

    /**
     * Restores all posts belonging to a specific user by their ID.
     *
     * @param userId ID of the user whose posts will be restored.
     * @return ResponseEntity containing a list of restored PostResponseDto.
     */
    @Transactional
    public ResponseEntity<List<PostResponseDto>> restoreAllByUserId(Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to restore posts by user ID: {}", userId);

        List<Post> posts = postRepository.findAllByUserId(userId);
        if (posts.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No posts found for user ID: {}", userId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        customLog.info(CUSTOM_LOG_MARKER, "Sending request to hash-service for restore hashes");
        List<Integer> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        List<HashResponseDto> hashes = hashClient.restoreAllHashesByPostsId(postIds).getBody();

        assert hashes != null;
        Map<Integer, String> hashMap = hashes.stream()
                .filter(hash -> postIds.contains(hash.postId()))
                .collect(Collectors.toMap(HashResponseDto::postId, HashResponseDto::hash));

        posts.forEach(post -> {
            post.setIsDeleted(false);
            post.setDeletedAt(null);
        });

        postRepository.saveAll(posts);

        List<PostResponseDto> restoredPosts = posts.stream()
                .map(post -> {
                    PostResponseDto responseDto = converter.convertToPostResponse(post);
                    if (hashMap.containsKey(post.getId())) {
                        responseDto.setHash(hashMap.get(post.getId()));
                    }
//                    responseDto.setTags(post.getTags().stream()
//                            .map(tag -> new TagResponseDto(tag.getName()))
//                            .collect(Collectors.toList()));
                    return responseDto;
                })
                .collect(Collectors.toList());

        customLog.info(CUSTOM_LOG_MARKER, "Sending to search-service for updating in posts index");
        posts.stream()
                .map(converter::convertToPostIndex)
                .forEach(kafkaProducer::sendMessageToPostIndexTopic);

        customLog.info(CUSTOM_LOG_MARKER, "Posts with user ID: {} restored", userId);
        return new ResponseEntity<>(restoredPosts, HttpStatus.OK);
    }
}
