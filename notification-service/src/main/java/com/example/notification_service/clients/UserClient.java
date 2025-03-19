package com.example.notification_service.clients;

import com.example.notification_service.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserClient {

    @GetMapping("{id}")
    ResponseEntity<UserResponseDto> getUserById(@PathVariable Integer id);

}
