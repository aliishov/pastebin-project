package com.raul.paste_service.services.postServices;

import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.models.PostLike;
import com.raul.paste_service.repositories.PostLikeRepository;
import com.raul.paste_service.repositories.PostRepository;
import com.raul.paste_service.utils.exceptions.PostNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final PostConverter postConverter;
    private static final Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    /**
     * Adds a like to the post by its ID.
     *
     * @param postId ID of the post to like.
     * @param userId ID of the user who likes the post.
     * @return ResponseEntity with status CREATED if successful.
     */
    public ResponseEntity<Void> likePost(Integer postId, Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "User {} is attempting to like post {}", userId, postId);

        if (postLikeRepository.existsByPost_IdAndUserId(postId, userId)) {
            customLog.warn(CUSTOM_LOG_MARKER, "User {} already liked post {}", userId, postId);
            throw new IllegalStateException("User already liked this post");
        }

        var post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "Post {} not found", postId);
                    return new PostNotFoundException("Post not found");
                });

        var like = PostLike.builder()
                .post(post)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        postLikeRepository.save(like);
        customLog.info(CUSTOM_LOG_MARKER, "User {} successfully liked post {}", userId, postId);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Removes a like from the post by its ID.
     *
     * @param postId ID of the post to unlike.
     * @param userId ID of the user who unlikes the post.
     * @return ResponseEntity with status OK if successful.
     */
    public ResponseEntity<Void> unlikePost(Integer postId, Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "User {} is attempting to unlike post {}", userId, postId);

        var like = postLikeRepository.findByPost_IdAndUserId(postId, userId)
                .orElseThrow(() -> {
                    customLog.warn(CUSTOM_LOG_MARKER, "Like not found for user {} on post {}", userId, postId);
                    return new EntityNotFoundException("Like not found");
                });

        postLikeRepository.delete(like);
        customLog.info(CUSTOM_LOG_MARKER, "User {} successfully unliked post {}", userId, postId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Retrieves all posts liked by a specific user.
     *
     * @param userId ID of the user.
     * @return List of liked posts.
     */
    public ResponseEntity<List<PostResponseDto>> getLikedPostsByUser(Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Fetching liked posts for user {}", userId);

        var likedPosts = postLikeRepository.findByUserId(userId);

        if (likedPosts.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "No liked post found for user with ID {}", userId);
            throw new PostNotFoundException("Posts not found");
        }

        List<PostResponseDto> likedPostsDto = likedPosts.stream()
                .map(postLike -> postConverter.convertToPostResponse(postLike.getPost()))
                .collect(Collectors.toList());

        return new ResponseEntity<>(likedPostsDto, HttpStatus.OK);
    }
}
