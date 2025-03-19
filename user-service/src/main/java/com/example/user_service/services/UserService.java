package com.example.user_service.services;

import com.example.user_service.clients.AuthServiceClient;
import com.example.user_service.clients.PasteServiceClient;
import com.example.user_service.dto.*;
import com.example.user_service.repositories.UserRepository;
import com.example.user_service.utils.exceptions.InvalidPasswordException;
import com.example.user_service.utils.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private static final Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");
    private final AuthServiceClient authServiceClient;
    private final PasteServiceClient pasteServiceClient;

    /**
     * Retrieves a user by their ID.
     *
     * @param id The ID of the user to retrieve.
     * @return ResponseEntity with the found UserResponseDto.
     */
    public ResponseEntity<UserResponseDto> getUserById(Integer id) {
        customLog.info(CUSTOM_LOG_MARKER, "Fetching user with ID: {}", id);

        var user = userRepository.findById(id)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "User with ID {} not found", id);
                    return new UserNotFoundException("User whit this ID not found");
                });

        customLog.info(CUSTOM_LOG_MARKER, "User with ID: {} found, returning user data", id);
        return new ResponseEntity<>(userConverter.convertToUserResponseDto(user), HttpStatus.OK);
    }

    /**
     * Uploads a profile photo for a user.
     *
     * @param file   The image file to upload.
     * @param userId The ID of the user uploading the photo.
     * @return ResponseEntity with a message indicating success.
     */
    public ResponseEntity<MessageResponse> uploadProfilePhoto(MultipartFile file, Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Attempting to upload profile photo for user with ID: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "User with ID {} not found", userId);
                    return new UserNotFoundException("User whit this ID not found");
                });

        customLog.info(CUSTOM_LOG_MARKER, "User with ID: {} found, saving profile photo", userId);

        String photoUrl = fileStorageService.saveFile(file);
        user.setImageUrl(photoUrl);
        userRepository.save(user);

        customLog.info(CUSTOM_LOG_MARKER, "Profile photo uploaded successfully for user with ID: {}. Photo URL: {}", userId, photoUrl);

        return new ResponseEntity<>(new MessageResponse("Profile photo uploaded successfully"),
                                    HttpStatus.OK);
    }

    /**
     * Updates user information.
     *
     * @param request UpdateUserRequest containing updated user details.
     * @param userId  The ID of the user to update.
     * @return ResponseEntity with the updated UserResponseDto.
     */
    public ResponseEntity<UserResponseDto> updateUser(UpdateUserRequest request, Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Updating user with ID: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "User with ID {} not found", userId);
                    return new UserNotFoundException("User whit this ID not found");
                });

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
            user.setIsAuthenticated(false);

            customLog.error(CUSTOM_LOG_MARKER, "Sending email confirmation request to auth-service");
            authServiceClient.resendConfirmation(new ResendConfirmationRequest(request.email()));
        }
        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }

        userRepository.save(user);

        customLog.info(CUSTOM_LOG_MARKER, "User with ID: {} updated successfully", userId);
        return ResponseEntity.ok(userConverter.convertToUserResponseDto(user));
    }

    /**
     * Updates the password of a user.
     *
     * @param request UpdatePasswordRequest containing the old and new password.
     * @param userId  The ID of the user updating their password.
     * @return ResponseEntity with a success message.
     */
    public ResponseEntity<MessageResponse> updatePassword(UpdatePasswordRequest request, Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Updating password for user with ID: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "User with ID {} not found", userId);
                    return new UserNotFoundException("User whit this ID not found");
                });

        customLog.info(CUSTOM_LOG_MARKER, "User found. Verifying current password...");

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            customLog.warn(CUSTOM_LOG_MARKER, "Invalid current password for user with ID: {}", userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }

        customLog.info(CUSTOM_LOG_MARKER, "Current password verified. Updating to new password...");

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        customLog.info(CUSTOM_LOG_MARKER, "Password updated successfully for user with ID: {}", userId);

        return new ResponseEntity<>(new MessageResponse("Password updated successfully"),
                                    HttpStatus.OK);
    }

    /**
     * Marks a user as deleted.
     *
     * @param userId The ID of the user to delete.
     * @return ResponseEntity with no content.
     */
    public ResponseEntity<Void> deleteUser(Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to delete user by ID: {}", userId);

        pasteServiceClient.deleteAllPostByUserId(userId);

        userRepository.markAsDeletedById(userId, LocalDateTime.now());

        customLog.info(CUSTOM_LOG_MARKER, "User with ID: {} deleted", userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Restores a deleted user.
     *
     * @param request UserRestoreDto containing the user's email and password.
     * @return ResponseEntity with a success message.
     */
    @Transactional
    public ResponseEntity<MessageResponse> restoreUser(UserRestoreDto request) {
        customLog.info(CUSTOM_LOG_MARKER, "Restoring user with email: {}", request.email());

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "User with email {} not found", request.email());
                    return new UserNotFoundException("User with this email not found");
                });

        if (!user.getIsDeleted()) {
            customLog.error(CUSTOM_LOG_MARKER, "User with email {} not deleted", request.email());
            throw new IllegalStateException("User is not deleted");
        }

        if (!passwordCheck(user.getPasswordHash(), request.password())) {
            customLog.error(CUSTOM_LOG_MARKER, "Incorrect password");
            throw new IllegalStateException("Incorrect password");
        }

        user.setIsDeleted(false);
        user.setDeletedAt(null);
        user.setIsAuthenticated(false);

        userRepository.save(user);

        try {
            pasteServiceClient.restoreAllByUserId(user.getId());
        } catch (Exception e) {
            customLog.error(CUSTOM_LOG_MARKER, "Failed to restore posts for user {}", user.getId(), e);
            throw new IllegalStateException("Failed to restore user posts, user restoration cancelled.");
        }

        customLog.info(CUSTOM_LOG_MARKER, "Sending email confirmation request to auth-service");
        authServiceClient.resendConfirmation(new ResendConfirmationRequest(request.email()));

        String message = "User with email " + request.email() + " successfully restored. Please confirm your email.";

        return new ResponseEntity<>(new MessageResponse(message), HttpStatus.OK);
    }

    private boolean passwordCheck(String passwordHash, String rawPassword) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
