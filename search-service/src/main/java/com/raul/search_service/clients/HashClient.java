package com.raul.search_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hash-service", url = "${hash.service.url}")
public interface HashClient {

    @GetMapping("/hash/{postId}")
    ResponseEntity<String> getHashByPostId(@PathVariable Integer postId);
}
