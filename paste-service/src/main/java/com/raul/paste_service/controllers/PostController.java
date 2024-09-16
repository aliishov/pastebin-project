package com.raul.paste_service.controllers;

import com.raul.paste_service.dto.PostRequestDto;
import com.raul.paste_service.dto.PostResponseDto;
import com.raul.paste_service.services.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/create")
    public ResponseEntity<PostResponseDto> create(@RequestBody @Valid PostRequestDto request) {
        return postService.create(request);
    }

    @GetMapping()
    public ResponseEntity<Map<Integer, PostResponseDto>> findAll() {
        return postService.findAll();
    }
}
