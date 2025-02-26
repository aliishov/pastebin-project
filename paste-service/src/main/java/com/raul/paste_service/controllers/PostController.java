package com.raul.paste_service.controllers;

import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.services.postServices.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/create")
    public ResponseEntity<PostResponseDto> create(@RequestBody @Valid PostRequestDto request) {
        return postService.create(request);
    }

    @GetMapping("/{hash}")
    public ResponseEntity<PostResponseDto> getPostByHash(@PathVariable String hash) {
        return postService.getPostByHash(hash);
    }

    @GetMapping()
    public ResponseEntity<PostResponseDto> getPostBySlug(@RequestParam(name = "slug") String slug) {
        return postService.getPostBySlug(slug);
    }

    @PatchMapping("/{postId}/addLike")
    public ResponseEntity<PostResponseDto> addLike(@PathVariable Integer postId) {
        return postService.addLike(postId);
    }

    @PatchMapping("/delete")
    public ResponseEntity<Void> deletePost(@RequestParam(name = "postId") Integer postId) {
        return postService.deletePost(postId);
    }

    @PutMapping("/delete-all")
    public ResponseEntity<Void> deleteAllPostByUserId(@RequestBody Integer userId) {
        return postService.deleteAllPostByUserId(userId);
    }
}
