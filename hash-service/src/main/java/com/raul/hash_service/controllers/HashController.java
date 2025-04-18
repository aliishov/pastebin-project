package com.raul.hash_service.controllers;

import com.raul.hash_service.dto.HashResponseDto;
import com.raul.hash_service.dto.PostIdDto;
import com.raul.hash_service.services.HashService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("api/v1/hashes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Hash Controller", description = "Manages hashes in the Hash Service")
public class HashController {

    private final HashService hashService;

    @Operation(summary = "Get post ID by hash", description = "Retrieves a post ID by its unique hash.")
    @ApiResponse(responseCode = "200", description = "Hash found",
            content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "Hash not found")
    @GetMapping("/{hash}")
    public ResponseEntity<Integer> getPostIdByHash(@PathVariable String hash) {
        return hashService.getPostIdByHash(hash);
    }

    @Operation(summary = "Get hash by post ID", description = "Retrieves a hash by its post ID.")
    @ApiResponse(responseCode = "200", description = "Hash found",
            content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "404", description = "Hash not found")
    @GetMapping("/hash/{postId}")
    public ResponseEntity<String> getHashByPostId(@PathVariable Integer postId) {
        return hashService.getHashByPostId(postId);
    }

    @Operation(summary = "Generate a new hash", description = "Generate a new hash and returns the generated hash details.")
    @ApiResponse(responseCode = "201", description = "Hash generated successfully",
            content = @Content(schema = @Schema(implementation = String.class)))
    @PostMapping("/generate-hash")
    public ResponseEntity<String> generateHash(@RequestBody @Valid PostIdDto request) {
        return hashService.generateHash(request);
    }

    @Operation(summary = "Soft delete a hash by post ID", description = "Marks a hash as deleted instead of physically removing it.")
    @ApiResponse(responseCode = "204", description = "Hash marked as deleted")
    @ApiResponse(responseCode = "404", description = "Hash not found")
    @PutMapping("/delete-hash")
    public ResponseEntity<Void> deleteHash(@RequestBody @Valid PostIdDto request) {
        return hashService.deleteHash(request);
    }

    @Operation(summary = "Restore all hashes by posts IDs", description = "Restores all hashes of a specific posts.")
    @ApiResponse(responseCode = "200", description = "Hashes restored successfully",
            content = @Content(schema = @Schema(implementation = HashResponseDto.class)))
    @PutMapping("/restore-all")
    public ResponseEntity<List<HashResponseDto>> restoreAllHashesByPostsId(@RequestBody List<Integer> postIds) {
        return hashService.restoreAllHashesByPostsId(postIds);
    }

    @Operation(summary = "Restore hash by posts IDs", description = "Restore hash of a specific posts.")
    @ApiResponse(responseCode = "200", description = "Hash restored successfully",
            content = @Content(schema = @Schema(implementation = HashResponseDto.class)))
    @PutMapping("/restore/{postId}")
    public ResponseEntity<HashResponseDto> restoreHashByPostId(@PathVariable Integer postId) {
        return hashService.restoreHashByPostId(postId);
    }
}
