package com.raul.paste_service.controllers;

import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.services.PostService;
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
    public ResponseEntity<PostResponseDto> create(@RequestBody @Valid PostRequestDto request) throws InterruptedException {
        return postService.create(request);
    }

    @GetMapping("/random")
    public ResponseEntity<PostResponseDto> getRandomPost() {
        return postService.getRandomPost();
    }

    @GetMapping("/{hash}")
    public ResponseEntity<PostResponseDto> getPostByHash(@PathVariable String hash) {
        return postService.getPostByHash(hash);
    }

    @PatchMapping("/{postId}/addLike")
    public ResponseEntity<PostResponseDto> addLike(@PathVariable Integer postId) {
        return postService.addLike(postId);
    }
}
