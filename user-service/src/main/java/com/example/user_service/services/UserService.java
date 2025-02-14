package com.example.user_service.services;

import com.example.user_service.dto.MessageResponse;
import com.example.user_service.dto.UpdatePasswordRequest;
import com.example.user_service.dto.UpdateUserRequest;
import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.dto.notification.EmailNotificationDto;
import com.example.user_service.dto.notification.EmailNotificationSubject;
import com.example.user_service.model.TokenType;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.utils.exceptions.InvalidPasswordException;
import com.example.user_service.utils.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final FileStorageService fileStorageService;
    private final KafkaProducer kafkaProducer;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public ResponseEntity<UserResponseDto> getUserById(Integer id) {
        log.info("Fetching user with ID: {}", id);

        var user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", id);
                    return new UserNotFoundException("User whit this ID not found");
                });

        log.info("User with ID: {} found, returning user data", id);
        return new ResponseEntity<>(userConverter.convertToUserResponseDto(user), HttpStatus.OK);
    }

    public ResponseEntity<MessageResponse> uploadProfilePhoto(MultipartFile file, Integer userId) {
        log.info("Attempting to upload profile photo for user with ID: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", userId);
                    return new UserNotFoundException("User whit this ID not found");
                });

        log.info("User with ID: {} found, saving profile photo", userId);

        String photoUrl = fileStorageService.saveFile(file);
        user.setImageUrl(photoUrl);
        userRepository.save(user);

        log.info("Profile photo uploaded successfully for user with ID: {}. Photo URL: {}", userId, photoUrl);

        return new ResponseEntity<>(new MessageResponse("Profile photo uploaded successfully"),
                                    HttpStatus.OK);
    }

    public ResponseEntity<UserResponseDto> updateUser(UpdateUserRequest request, Integer userId) {
        log.info("Updating user with ID: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with this ID not found"));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());

            String token = tokenService.generateToken(user.getId(), TokenType.EMAIL_CONFIRMATION_TOKEN);
            String confirmationLink = "http://localhost:8010/api/v1/auth/confirm-email?token=" + token;

            Map<String, String> placeholders = Map.of(
                    "confirmation_link", confirmationLink
            );

            kafkaProducer.sendMessageToAuthNotificationTopic(new EmailNotificationDto(
                    user.getId(),
                    EmailNotificationSubject.EMAIL_CONFIRMATION_NOTIFICATION,
                    placeholders
            ));
        }
        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }

        userRepository.save(user);

        log.info("User with ID: {} updated successfully", userId);
        return ResponseEntity.ok(userConverter.convertToUserResponseDto(user));
    }

    public ResponseEntity<MessageResponse> updatePassword(UpdatePasswordRequest request, Integer userId) {
        log.info("Updating password for user with ID: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with this ID not found"));

        log.info("User found. Verifying current password...");

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Invalid current password for user with ID: {}", userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }

        log.info("Current password verified. Updating to new password...");

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password updated successfully for user with ID: {}", userId);

        return new ResponseEntity<>(new MessageResponse("Password updated successfully"),
                                    HttpStatus.OK);
    }
}
