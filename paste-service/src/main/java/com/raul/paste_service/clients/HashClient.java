package com.raul.paste_service.clients;

import com.raul.paste_service.dto.PostIdDto;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "hash-service", url = "${hash.service.url}")
public interface HashClient {
    @GetMapping("/{hash}")
    ResponseEntity<Integer> getPostIdByHash(@PathVariable String hash);

    @GetMapping("/hash/{postId}")
    ResponseEntity<String> getHashByPostId(@PathVariable Integer postId);

    @PostMapping("/generate-hash")
    ResponseEntity<String> generateHash(@RequestBody @Valid PostIdDto request);

    @PutMapping("/delete-hash")
    ResponseEntity<Void> deleteHash(@RequestBody @Valid PostIdDto request);
}
