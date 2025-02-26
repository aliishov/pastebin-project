package com.example.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "paste-service", url = "${paste.service.url}")
public interface PasteServiceClient {

    @PutMapping("/delete-all")
    ResponseEntity<Void> deleteAllPostByUserId(@RequestBody Integer userId);
}
