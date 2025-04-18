package com.raul.paste_service.controllers;

import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.services.postServices.PostLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/posts/likes")
@RequiredArgsConstructor
@Validated
@Tag(name = "PostLike Controller", description = "Manages posts likes in the Paste Service")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @Operation(summary = "Like a post", description = "Adds a like to the specified post by the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Like added successfully")
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Integer postId,
                                         @RequestHeader("X-User-Id") String userId) {
        return postLikeService.likePost(postId, userId);
    }

    @Operation(summary = "Unlike a post", description = "Removes a like from the specified post by the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Posts unlike successfully")
    @ApiResponse(responseCode = "404", description = "Posts not found")
    @DeleteMapping("/{postId}/unlike")
    public ResponseEntity<Void> unlikePost(@PathVariable Integer postId,
                                           @RequestHeader("X-User-Id") String userId) {
        return postLikeService.unlikePost(postId, userId);
    }

    @Operation(summary = "Get liked posts by user", description = "Retrieves all posts liked by the specified user.")
    @ApiResponse(responseCode = "200", description = "Posts found",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Posts not found")
    @GetMapping("/user/{pathUserId}")
    public ResponseEntity<List<PostResponseDto>> getLikedPostsByUser(
            @Parameter(description = "ID of the user") @PathVariable Integer pathUserId,
            @RequestHeader("X-User-Id") String headerUserId) {
        return postLikeService.getLikedPostsByUser(pathUserId, headerUserId);
    }
}
