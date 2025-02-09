package com.raul.auth_service.service;

import com.raul.auth_service.dto.*;
import com.raul.auth_service.dto.notification.EmailNotificationDto;
import com.raul.auth_service.dto.notification.EmailNotificationSubject;
import com.raul.auth_service.model.Role;
import com.raul.auth_service.model.TokenType;
import com.raul.auth_service.model.User;
import com.raul.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final KafkaProducer kafkaProducer;
    private final TokenService tokenService;

    public ResponseEntity<MessageResponse> register(RegisterRequest request) {
        log.info("Registering a new user with email: {}", request.getEmail());
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .role(Role.USER)
                .isActive(true)
                .isAuthenticated(false)
                .isDeleted(false)
                .build();

        userRepository.save(user);

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

        log.info("User with email: {} has been registered successfully.", request.getEmail());

        return new ResponseEntity<>(new MessageResponse("User register successfully")
                                    , HttpStatus.CREATED);

    }

    public ResponseEntity<AuthenticationResponse> login(AuthenticationRequest request) {
        log.info("Authenticating user with email: {}", request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User with email: " + request.getEmail() + " not found"));

        user.setIsAuthenticated(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User with email: {} has been successfully login and updated.", request.getEmail());

        var jwtToken = jwtService.generateToken(user);
        log.info("JWT token generated for authenticated user with email: {}", request.getEmail());

        return new ResponseEntity<>(AuthenticationResponse
                .builder()
                .token(jwtToken)
                .build(), HttpStatus.OK);
    }

    public ResponseEntity<MessageResponse> forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password for email: {}", request.email());

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = tokenService.generateToken(user.getId(), TokenType.FORGOT_PASSWORD_TOKEN);
        String resetLink = "http://localhost:8010/api/v1/auth/reset-password?token=" + token;

        Map<String, String> placeholders = Map.of(
                "reset_link", resetLink
        );

        kafkaProducer.sendMessageToAuthNotificationTopic(new EmailNotificationDto(
                user.getId(),
                EmailNotificationSubject.FORGOT_PASSWORD,
                placeholders
        ));

        log.info("Password reset email sent to {}", request.email());
        return new ResponseEntity<>(new MessageResponse("Password reset email sent"), HttpStatus.OK);
    }

    public ResponseEntity<MessageResponse> resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password");

        Integer userId;
        try {
            userId = tokenService.validateToken(request.token());
        } catch (Exception e) {
            log.warn("Invalid or expired token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User with ID {} not found", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User not found"));
        }

        User user = userOpt.get();

        String hashedPassword = passwordEncoder.encode(request.newPassword());

        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        log.info("Password successfully reset for user with email: {}", user.getEmail());
        return new ResponseEntity<>(new MessageResponse("Password successfully reset"), HttpStatus.OK);
    }

    public ResponseEntity<MessageResponse> confirmEmail(String token) {

        Integer userId;
        try {
            userId = tokenService.validateToken(token);
        } catch (Exception e) {
            log.warn("Invalid or expired token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(new MessageResponse(e.getMessage()));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User with ID {} not found", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(new MessageResponse("User not found"));
        }

        User user = userOpt.get();

        log.info("Confirming email: {}", user.getEmail());

        user.setIsAuthenticated(true);
        userRepository.save(user);

        log.info("Email confirmed successfully for {}", user.getEmail());
        return new ResponseEntity<>(new MessageResponse("Email successfully confirmed"), HttpStatus.OK);
    }
}
