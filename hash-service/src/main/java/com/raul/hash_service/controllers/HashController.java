package com.raul.hash_service.controllers;

import com.raul.hash_service.dto.PostIdDto;
import com.raul.hash_service.services.HashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/hashes")
@RequiredArgsConstructor
public class HashController {

    private final HashService hashService;

    @GetMapping("/{hash}")
    public ResponseEntity<Integer> getPostIdByHash(@PathVariable String hash) {
        return hashService.getPostIdByHash(hash);
    }

    @GetMapping("/hash/{postId}")
    public ResponseEntity<String> getHashByPostId(@PathVariable Integer postId) {
        return hashService.getHashByPostId(postId);
    }

    @PostMapping("/generate-hash")
    public ResponseEntity<String> generateHash(@RequestBody @Valid PostIdDto request) {
        return hashService.generateHash(request);
    }
}
