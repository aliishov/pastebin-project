package com.example.user_service.controllers;

import com.example.user_service.dto.*;
import com.example.user_service.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Controller", description = "Manages users in the User Service")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user by ID", description = "Retrieve user details by their ID")
    @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(
            @Parameter(description = "ID of the user") @PathVariable Integer id) {
        return userService.getUserById(id);
    }
    @Operation(summary = "Upload profile photo", description = "Upload a profile photo for a user")
    @ApiResponse(responseCode = "200", description = "Profile photo uploaded successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping(value = "/{userId}/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadProfilePhoto(
            @Parameter(description = "Profile photo") @RequestPart("file") MultipartFile file,
            @Parameter(description = "ID of the user") @PathVariable Integer userId) {
        return userService.uploadProfilePhoto(file, userId);
    }

    @Operation(summary = "Update user details", description = "Modify user details like name or other profile information")
    @ApiResponse(responseCode = "200", description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "User not found")
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponseDto> updateUser(
            @RequestBody UpdateUserRequest request,
            @Parameter(description = "ID of the user") @PathVariable Integer userId) {
        return userService.updateUser(request, userId);
    }

    @Operation(summary = "Update user password", description = "Change the password for a given user")
    @ApiResponse(responseCode = "200", description = "Password updated successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @PatchMapping("/{userId}/password")
    public ResponseEntity<MessageResponse> updatePassword(
            @RequestBody @Valid UpdatePasswordRequest request,
            @Parameter(description = "ID of the user") @PathVariable Integer userId) {
        return userService.updatePassword(request, userId);
    }

    @Operation(summary = "Soft delete user", description = "Marks a user as deleted")
    @ApiResponse(responseCode = "204", description = "User marked as deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PatchMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user") @PathVariable Integer userId) {
        return userService.deleteUser(userId);
    }

    @Operation(summary = "Restore user account", description = "Restore a previously deleted user account")
    @ApiResponse(responseCode = "200", description = "User restored successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "404", description = "user not found")
    @PutMapping("/restore")
    public ResponseEntity<MessageResponse> restoreUser(@RequestBody @Valid UserRestoreDto request) {
        return userService.restoreUser(request);
    }
}
