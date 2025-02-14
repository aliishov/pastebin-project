package com.example.user_service.controller;

import com.example.user_service.dto.MessageResponse;
import com.example.user_service.dto.UpdatePasswordRequest;
import com.example.user_service.dto.UpdateUserRequest;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Integer id) {
        return userService.getUserById(id);
    }

    @PostMapping(value = "/upload-profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadProfilePhoto(@RequestPart("file") MultipartFile file,
                                                     @RequestParam("userId") Integer userId) {

        return userService.uploadProfilePhoto(file, userId);
    }

    @PatchMapping("update")
    public ResponseEntity<UserResponseDto> updateUser(@RequestBody UpdateUserRequest request, @RequestParam Integer userId) {
        return userService.updateUser(request, userId);
    }

    @PatchMapping("update-password")
    public ResponseEntity<MessageResponse> updatePassword(@RequestBody @Valid UpdatePasswordRequest request, @RequestParam Integer userId) {
        return userService.updatePassword(request, userId);
    }
}
