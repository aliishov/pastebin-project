package com.example.user_service.client;

import com.example.user_service.dto.PostResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "paste-service", url = "${paste.service.url}")
public interface PasteServiceClient {

    @PutMapping("/user/{userId}")
    ResponseEntity<Void> deleteAllPostByUserId(@PathVariable Integer userId);

    @PutMapping("/user/{userId}/restore")
    ResponseEntity<List<PostResponseDto>> restoreAllByUserId(@PathVariable Integer userId);
}
