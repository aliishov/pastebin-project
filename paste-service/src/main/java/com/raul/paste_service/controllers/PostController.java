package com.raul.paste_service.controllers;

import com.raul.paste_service.dto.post.PostRequestDto;
import com.raul.paste_service.dto.post.PostResponseDto;
import com.raul.paste_service.services.postServices.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/posts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Post Controller", description = "Manages posts in the Paste Service")
public class PostController {

    private final PostService postService;

    @Operation(summary = "Create a new post", description = "Creates a new post and returns the created post details.")
    @ApiResponse(responseCode = "201", description = "Post created successfully",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @PostMapping
    public ResponseEntity<PostResponseDto> create(@RequestBody @Valid PostRequestDto request,
                                                  @RequestHeader("X-User-Id") String userId) {
        return postService.create(request, userId);
    }

    @Operation(summary = "Get post by hash", description = "Retrieves a post by its unique hash.")
    @ApiResponse(responseCode = "200", description = "Post found",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Post not found")
    @GetMapping("/{hash}")
    public ResponseEntity<PostResponseDto> getPostByHash(
            @Parameter(description = "Unique hash of the post") @PathVariable String hash, HttpServletRequest request) {
        return postService.getPostByHash(hash, request);
    }

    @Operation(summary = "Get post by slug", description = "Retrieves a post using its slug.")
    @ApiResponse(responseCode = "200", description = "Post found",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Post not found")
    @GetMapping()
    public ResponseEntity<PostResponseDto> getPostBySlug(
            @Parameter(description = "Slug of the post") @RequestParam(name = "slug") String slug, HttpServletRequest request) {
        return postService.getPostBySlug(slug, request);
    }

    @Operation(summary = "Soft delete a post", description = "Marks a post as deleted instead of physically removing it.")
    @ApiResponse(responseCode = "204", description = "Post marked as deleted")
    @ApiResponse(responseCode = "404", description = "Post not found")
    @PatchMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "ID of the post to delete") @PathVariable Integer postId,
            @RequestHeader("X-User-Id") String userId) {
        return postService.deletePost(postId, userId);
    }

    @Operation(summary = "Soft delete all posts by user ID", description = "Marks all posts of a specific user as deleted.")
    @ApiResponse(responseCode = "204", description = "All posts marked as deleted")
    @ApiResponse(responseCode = "404", description = "No posts found for the user")
    @PutMapping("/user/{pathUserId}")
    public ResponseEntity<Void> deleteAllPostByUserId(
            @Parameter(description = "ID of the user") @PathVariable Integer pathUserId,
            @RequestHeader("X-User-Id") String headerUserId) {
        return postService.deleteAllPostByUserId(pathUserId, headerUserId);
    }

    @Operation(summary = "Restore all posts by user ID", description = "Restores all posts of a specific user.")
    @ApiResponse(responseCode = "200", description = "Posts restored successfully",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "No deleted posts found for the user")
    @PutMapping("/user/{pathUserId}/restore")
    public ResponseEntity<List<PostResponseDto>> restoreAllByUserId(
            @Parameter(description = "ID of the user") @PathVariable Integer pathUserId,
            @RequestHeader("X-User-Id") String headerUserId) {
        return  postService.restoreAllByUserId(pathUserId, headerUserId);
    }

    @Operation(summary = "Get posts by user ID", description = "Retrieves a posts using its user ID.")
    @ApiResponse(responseCode = "200", description = "Posts found",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Posts not found")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponseDto>> getPostsByUser(
            @Parameter(description = "ID of the user") @PathVariable Integer userId) {
        return postService.getPostsByUserId(userId);
    }

    @Operation(summary = "Get deleted posts by user ID", description = "Retrieves a deleted posts using its user ID.")
    @ApiResponse(responseCode = "200", description = "Posts found",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Posts not found")
    @GetMapping("/user/{pathUserId}/garbage")
    public ResponseEntity<List<PostResponseDto>> getDeletedPostsByUser(
            @Parameter(description = "ID of the user") @PathVariable Integer pathUserId,
            @RequestHeader("X-User-Id") String headerUserId) {
        return postService.getDeletedPostsByUserId(pathUserId, headerUserId);
    }
}
