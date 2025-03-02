package com.example.user_service.services;

import com.example.user_service.client.AuthServiceClient;
import com.example.user_service.client.PasteServiceClient;
import com.example.user_service.dto.MessageResponse;
import com.example.user_service.dto.UpdatePasswordRequest;
import com.example.user_service.dto.UpdateUserRequest;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.dto.ResendConfirmationRequest;
import com.example.user_service.repository.UserRepository;
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

    public ResponseEntity<Void> deleteUser(Integer userId) {
        customLog.info(CUSTOM_LOG_MARKER, "Received request to delete user by ID: {}", userId);

        pasteServiceClient.deleteAllPostByUserId(userId);

        userRepository.markAsDeletedById(userId, LocalDateTime.now());

        customLog.info(CUSTOM_LOG_MARKER, "User with ID: {} deleted", userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Transactional
    public ResponseEntity<MessageResponse> restoreUser(String email) {
        customLog.info(CUSTOM_LOG_MARKER, "Restoring user with email: {}", email);

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    customLog.error(CUSTOM_LOG_MARKER, "User with email {} not found", email);
                    return new UserNotFoundException("User whit this email not found");
                });

        if (!user.getIsDeleted()) {
            customLog.error(CUSTOM_LOG_MARKER, "User with email {} not deleted", email);
            throw new IllegalStateException("User is not deleted");
        }

        user.setIsDeleted(false);
        user.setDeletedAt(null);
        user.setIsAuthenticated(false);

        userRepository.save(user);

        pasteServiceClient.restoreAllByUserId(user.getId());

        customLog.info(CUSTOM_LOG_MARKER, "Sending email confirmation request to auth-service");
        authServiceClient.resendConfirmation(new ResendConfirmationRequest(email));

        String message = "User with email " + email + " successfully restored. Please confirm your email.";

        return new ResponseEntity<>(new MessageResponse(message), HttpStatus.ACCEPTED);
    }
}
