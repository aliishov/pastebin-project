package com.raul.search_service.controllers;

import com.raul.search_service.dto.PostResponseDto;
import com.raul.search_service.services.PostIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/p")
@RequiredArgsConstructor
@Validated
@Tag(name = "Post Search Controller", description = "Endpoints for searching posts in Elasticsearch")
public class PostIndexController {

    private final PostIndexService postIndexService;

    @Operation(summary = "Search posts", description = "Search posts by query")
    @ApiResponse(responseCode = "200", description = "Posts found",
            content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Posts not found")
    @GetMapping("/search")
    public ResponseEntity<List<PostResponseDto>> search(@RequestParam("query") String query,
                                                        @RequestParam(name = "page", defaultValue = "0") Integer page,
                                                        @RequestParam(name = "size", defaultValue = "10") Integer size) {
        return postIndexService.search(query, page, size);
    }
}
