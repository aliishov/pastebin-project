package com.raul.paste_service.services.postServices;


import com.raul.paste_service.dto.post.PostRequestDto;
import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.dto.post.RestorePostDto;
import com.raul.paste_service.models.Post;
import com.raul.paste_service.models.Tag;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.repositories.TagRepository;
import com.raul.paste_service.services.UserAccessService;
import com.raul.paste_service.services.kafkaServices.KafkaProducer;
import com.raul.paste_service.services.schedulerServices.PostCleanUpService;
import com.raul.paste_service.utils.exceptions.PostNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostConverter converter;
    private static final Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");
    private final TagRepository tagRepository;
    private final KafkaProducer kafkaProducer;
    private final PostCleanUpService postCleanUpService;
    private static final String POSTS_CACHE = "posts::";
    private final RedisTemplate<String, Post> redisTemplate;
    private final CacheManager cacheManager;
    private final PostViewService postViewService;
    private final UserAccessService userAccessService;

    /**
     * Creates a new post and saves it to the database.
     *
     * @param request Post request DTO.
     * @return ResponseEntity with created PostResponseDto.
     */
    @Transactional
    public ResponseEntity<PostResponseDto> create(PostRequestDto request, String userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Creating new post");

        Post post = converter.convertToPost(request, Integer.parseInt(userId));

        postRepository.save(post);

        customLog.info(CUSTOM_LOG_MARKER, "Saving tags");
        savePostTags(post, request.tags());

        PostResponseDto postResponse = converter.convertToPostResponse(post);

        customLog.info(CUSTOM_LOG_MARKER, "Sending to search-service");
        kafkaProducer.sendMessageToPostIndexTopic(converter.convertToPostIndex(post));

        customLog.info(CUSTOM_LOG_MARKER, "Post created with ID: {}", post.getId());
        return new ResponseEntity<>(postResponse, HttpStatus.CREATED);
    }

    /**
     * Retrieves a post
     *
     * @param postId ID of the post
     * @param hash Unique hash of the post
     * @param slug slug of the post
     * @return ResponseEntity with the found PostResponseDto
     */
    public ResponseEntity<PostResponseDto> getPost(Integer postId, String hash, String slug, HttpServletRequest request) {
        Post post;

        if (postId != null) {
            post = redisTemplate.opsForValue().get(POSTS_CACHE + postId);
            if (post == null) {
                post = postRepository.findByIdAndIsDeletedFalse(postId)
                        .orElseThrow(() -> new PostNotFoundException("Post not found"));
            }
        } else if (hash != null) {
            post = postRepository.findByHashAndIsDeletedFalse(hash)
                    .orElseThrow(() -> new PostNotFoundException("Post not found"));
        } else if (slug != null) {
            post = postRepository.findBySlugAndIsDeletedFalse(slug)
                    .orElseThrow(() -> new PostNotFoundException("Post not found"));
        } else {
            throw new BadRequestException("At least one identifier (id, hash, slug) must be provided");
        }

        Integer userId = getUserIdHeader(request);
        postViewService.handleView(post.getId(), userId, request);

        return new ResponseEntity<>(converter.convertToPostResponse(post), HttpStatus.OK);
    }

    /**
     * Retrieves a post by its hash.
     *
     * @param hash Hash to search for.
     * @return ResponseEntity with the found PostResponseDto.
     */
    public ResponseEntity<PostResponseDto> getPostByHash(String hash, HttpServletRequest request) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to find post by hash: {}", hash);

        Integer userId = getUserIdHeader(request);

        var post = postRepository.findByHashAndIsDeletedFalse(hash)
                    .orElseThrow(() -> new PostNotFoundException("Post not found"));

        postViewService.handleView(post.getId(), userId, request);

        PostResponseDto postResponseDto = converter.convertToPostResponse(post);
        return new ResponseEntity<>(postResponseDto, HttpStatus.OK);
    }

    /**
     * Retrieves a post by its slug.
     *
     * @param slug Slug to search for.
     * @return ResponseEntity with the found PostResponseDto.
     */
    public ResponseEntity<PostResponseDto> getPostBySlug(String slug, HttpServletRequest request) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to find post by slug: {}", slug);

        var post = postRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        PostResponseDto postResponseDto = converter.convertToPostResponse(post);

        Integer userId = getUserIdHeader(request);

        postViewService.handleView(post.getId(), userId, request);

        return new ResponseEntity<>(postResponseDto, HttpStatus.OK);
    }

    /**
     * Deletes a single post by its ID.
     *
     * @param postId The ID of the post to delete.
     * @return ResponseEntity with status NO_CONTENT if deletion is successful, CONFLICT if post is already deleted.
     */
    @CacheEvict(value = POSTS_CACHE, key = "#postId")
    @Transactional
    public ResponseEntity<Void> deletePost(Integer postId, String userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to delete post by post ID: {}", postId);

        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        userAccessService.userAccessCheck(post.getUserId(), userId);

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
     * @param pathUserId The ID of the user whose posts will be deleted.
     * @return ResponseEntity with status NO_CONTENT if deletion is successful, NOT_FOUND if no posts are found.
     */
    @Transactional
    public ResponseEntity<Void> deleteAllPostByUserId(Integer pathUserId, String headerUserId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to delete posts by user ID: {}", pathUserId);

        userAccessService.userAccessCheck(pathUserId, headerUserId);

        List<Post> posts = postRepository.findAllByUserId(pathUserId);
        if (posts.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No posts found for user ID: {}", pathUserId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        customLog.info(CUSTOM_LOG_MARKER, "Sending request to hash-service for mark hash as deleted");
        postCleanUpService.sendDeleteRequestToHashService(posts);

        Cache cache = cacheManager.getCache(POSTS_CACHE);
        if (cache != null) {
            posts.forEach(post -> cache.evict(post.getId()));
        }

        posts.forEach(post -> {
            post.setIsDeleted(true);
            post.setDeletedAt(LocalDateTime.now());
        });

        postRepository.saveAll(posts);

        customLog.info(CUSTOM_LOG_MARKER, "Sending to search-service for updating in posts index");
        posts.stream()
                .map(converter::convertToPostIndex)
                .forEach(kafkaProducer::sendMessageToPostIndexTopic);

        customLog.info(CUSTOM_LOG_MARKER, "Posts with user ID: {} marked as deleted", pathUserId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
     * @param pathUserId ID of the user whose posts will be restored.
     * @return ResponseEntity containing a list of restored PostResponseDto.
     */
    @Transactional
    public ResponseEntity<List<PostResponseDto>> restoreAllByUserId(Integer pathUserId, String headerUserId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to restore posts by user ID: {}", pathUserId);

        userAccessService.userAccessCheck(pathUserId, headerUserId);

        List<Post> posts = postRepository.findAllByUserId(pathUserId);
        if (posts.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No posts found for user ID: {}", pathUserId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        posts.forEach(post -> {
            LocalDateTime now = LocalDateTime.now();
            Duration expiredDuration = Duration.between(post.getExpiresAt(), post.getDeletedAt());
            LocalDateTime newExpirationDate = now.plus(expiredDuration);

            post.setExpiresAt(newExpirationDate);
            post.setIsDeleted(false);
            post.setDeletedAt(null);
        });

        postRepository.saveAll(posts);

        List<PostResponseDto> restoredPosts = posts.stream()
                .map(converter::convertToPostResponse)
                .collect(Collectors.toList());

        customLog.info(CUSTOM_LOG_MARKER, "Sending to search-service for updating in posts index");
        posts.stream()
                .map(converter::convertToPostIndex)
                .forEach(kafkaProducer::sendMessageToPostIndexTopic);

        customLog.info(CUSTOM_LOG_MARKER, "Posts with user ID: {} restored", pathUserId);
        return new ResponseEntity<>(restoredPosts, HttpStatus.OK);
    }

    /**
     * Retrieves all posts created by a specific user.
     *
     * @param userId ID of the user.
     * @return List of posts created by the user.
     */
    public ResponseEntity<List<PostResponseDto>> getPostsByUserId(Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Fetching all posts for user {}", userId);

        var posts = postRepository.findAllByUserIdAndIsDeletedFalse(userId);

        if (posts.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No post found for user with ID {}", userId);
            throw new PostNotFoundException("Posts not found");
        }

        List<PostResponseDto> userPosts = posts.stream()
                .map(converter::convertToPostResponse)
                .collect(Collectors.toList());

        return new ResponseEntity<>(userPosts, HttpStatus.OK);
    }

    /**
     * Retrieves all deleted posts created by a specific user.
     *
     * @param pathUserId ID of the user.
     * @return List of deleted posts created by the user.
     */
    public ResponseEntity<List<PostResponseDto>> getDeletedPostsByUserId(Integer pathUserId, String headerUserId) {
        customLog.info(CUSTOM_LOG_MARKER, "Fetching all deleted posts for user {}", pathUserId);

        userAccessService.userAccessCheck(pathUserId, headerUserId);

        var posts = postRepository.findByUserIdAndIsDeletedTrue(pathUserId);

        if (posts.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No deleted post found for user with ID {}", pathUserId);
            throw new PostNotFoundException("Posts not found");
        }

        List<PostResponseDto> userPosts = posts.stream()
                .map(converter::convertToPostResponse)
                .collect(Collectors.toList());

        return new ResponseEntity<>(userPosts, HttpStatus.OK);
    }

    /**
     * Retrieves ID of the user from request headers
     *
     * @param request Request Object
     * @return ID of the user
     */
    private Integer getUserIdHeader(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        Integer userId = null;
        if (userIdHeader != null) {
            try {
                userId = Integer.parseInt(userIdHeader);
            } catch (NumberFormatException ignored) {}
        }
        return userId;
    }

    /**
     * Restores a deleted post by its unique identifier.
     *
     * @param postId ID of the post
     * @param restorePostDto Object containing information, such as the
     *                        number of days to set as the new expiration time.
     * @return ResponseEntity containing the restored PostResponseDto.
     */
    @Transactional
    public ResponseEntity<PostResponseDto> restorePostById(Integer postId, RestorePostDto restorePostDto, String userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to restore post by post ID: {}", postId);

        var post = postRepository.findByIdAndIsDeletedTrue(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        userAccessService.userAccessCheck(post.getUserId(), userId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpirationDate = restorePostDto.days() != null
                ? now.plusDays(restorePostDto.days())
                : null;

        post.setIsDeleted(false);
        post.setDeletedAt(null);
        post.setExpiresAt(newExpirationDate);

        postRepository.save(post);

        customLog.info(CUSTOM_LOG_MARKER, "Sending to search-service for updating in posts index");
        kafkaProducer.sendMessageToPostIndexTopic(converter.convertToPostIndex(post));

        customLog.info(CUSTOM_LOG_MARKER, "Post with ID: {} restored", postId);
        return new ResponseEntity<>(converter.convertToPostResponse(post), HttpStatus.OK);
    }
}
