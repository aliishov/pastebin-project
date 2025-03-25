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
    public ResponseEntity<PostResponseDto> create(@RequestBody @Valid PostRequestDto request) {
        return postService.create(request);
    }

    @Operation(summary = "Get post by hash", description = "Retrieves a post by its unique hash.")
    @ApiResponse(responseCode = "200", description = "Post found",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Post not found")
    @GetMapping("/{hash}")
    public ResponseEntity<PostResponseDto> getPostByHash(
            @Parameter(description = "Unique hash of the post") @PathVariable String hash) {
        return postService.getPostByHash(hash);
    }

    @Operation(summary = "Get post by slug", description = "Retrieves a post using its slug.")
    @ApiResponse(responseCode = "200", description = "Post found",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Post not found")
    @GetMapping()
    public ResponseEntity<PostResponseDto> getPostBySlug(
            @Parameter(description = "Slug of the post") @RequestParam(name = "slug") String slug) {
        return postService.getPostBySlug(slug);
    }

    @Operation(summary = "Add a like to a post", description = "Increments the like count of a post by its ID.")
    @ApiResponse(responseCode = "200", description = "Like added successfully",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Post not found")
    @PatchMapping("/{postId}/addLike")
    public ResponseEntity<PostResponseDto> addLike(
            @Parameter(description = "ID of the post") @PathVariable Integer postId) {
        return postService.addLike(postId);
    }

    @Operation(summary = "Soft delete a post", description = "Marks a post as deleted instead of physically removing it.")
    @ApiResponse(responseCode = "204", description = "Post marked as deleted")
    @ApiResponse(responseCode = "404", description = "Post not found")
    @PatchMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "ID of the post to delete") @PathVariable Integer postId) {
        return postService.deletePost(postId);
    }

    @Operation(summary = "Soft delete all posts by user ID", description = "Marks all posts of a specific user as deleted.")
    @ApiResponse(responseCode = "204", description = "All posts marked as deleted")
    @ApiResponse(responseCode = "404", description = "No posts found for the user")
    @PutMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllPostByUserId(
            @Parameter(description = "ID of the user") @PathVariable Integer userId) {
        return postService.deleteAllPostByUserId(userId);
    }

    @Operation(summary = "Restore all posts by user ID", description = "Restores all posts of a specific user.")
    @ApiResponse(responseCode = "200", description = "Posts restored successfully",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "No deleted posts found for the user")
    @PutMapping("/user/{userId}/restore")
    public ResponseEntity<List<PostResponseDto>> restoreAllByUserId(
            @Parameter(description = "ID of the user") @PathVariable Integer userId) {
        return  postService.restoreAllByUserId(userId);
    }
}
