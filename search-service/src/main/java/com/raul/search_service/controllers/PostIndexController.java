package com.raul.search_service.controllers;

import com.raul.search_service.dto.PostResponseDto;
import com.raul.search_service.services.PostIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/p")
@RequiredArgsConstructor
public class PostIndexController {

    private final PostIndexService postIndexService;

    @GetMapping("/search")
    public ResponseEntity<List<PostResponseDto>> search(@RequestParam("query") String query,
                                                        @RequestParam(name = "page", defaultValue = "0") Integer page,
                                                        @RequestParam(name = "size", defaultValue = "10") Integer size) {
        return postIndexService.search(query, page, size);
    }
}
