package com.raul.auth_service.services;

import com.raul.auth_service.dto.*;
import com.raul.auth_service.dto.notification.EmailNotificationDto;
import com.raul.auth_service.dto.notification.EmailNotificationSubject;
import com.raul.auth_service.models.Role;
import com.raul.auth_service.models.TokenType;
import com.raul.auth_service.models.User;
import com.raul.auth_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    public ResponseEntity<MessageResponse> register(RegisterRequest request) {
        customLog.info(CUSTOM_LOG_MARKER, "Registering a new user with email: {}", request.getEmail());

        String nickname = (request.getNickname() == null || request.getNickname().trim().isEmpty()) ? null : request.getNickname();

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .nickname(nickname)
                .email(request.getEmail())
                .imageUrl(null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .role(Role.USER)
                .isActive(true)
                .isAuthenticated(false)
                .isDeleted(false)
                .deletedAt(null)
                .build();

        userRepository.save(user);

        String token = tokenService.generateToken(user.getId(), TokenType.EMAIL_CONFIRMATION_TOKEN);
        String confirmationLink = "http://localhost:8010/api/v1/auth/email/confirm?token=" + token;

        Map<String, String> placeholders = Map.of(
                "confirmation_link", confirmationLink
        );

        kafkaProducer.sendMessageToAuthNotificationTopic(new EmailNotificationDto(
                user.getId(),
                EmailNotificationSubject.EMAIL_CONFIRMATION_NOTIFICATION,
                placeholders
        ));

        customLog.info(CUSTOM_LOG_MARKER, "User with email: {} has been registered successfully.", request.getEmail());

        return new ResponseEntity<>(new MessageResponse("User register successfully")
                                    , HttpStatus.CREATED);

    }

    public ResponseEntity<AuthenticationResponse> login(AuthenticationRequest request) {
        customLog.info(CUSTOM_LOG_MARKER, "Authenticating user with email: {}", request.getEmail());
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
        customLog.info(CUSTOM_LOG_MARKER, "User with email: {} has been successfully login and updated.", request.getEmail());

        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        customLog.info(CUSTOM_LOG_MARKER, "JWT token generated for authenticated user with email: {}", request.getEmail());

        return new ResponseEntity<>(AuthenticationResponse
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build(), HttpStatus.OK);
    }

    public ResponseEntity<MessageResponse> forgotPassword(ForgotPasswordRequest request) {
        customLog.info(CUSTOM_LOG_MARKER, "Processing forgot password for email: {}", request.email());

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = tokenService.generateToken(user.getId(), TokenType.FORGOT_PASSWORD_TOKEN);
        String resetLink = "http://localhost:8010/api/v1/auth/password/reset?token=" + token;

        Map<String, String> placeholders = Map.of(
                "reset_link", resetLink
        );

        kafkaProducer.sendMessageToAuthNotificationTopic(new EmailNotificationDto(
                user.getId(),
                EmailNotificationSubject.FORGOT_PASSWORD,
                placeholders
        ));

        customLog.info(CUSTOM_LOG_MARKER, "Password reset email sent to {}", request.email());
        return new ResponseEntity<>(new MessageResponse("Password reset email sent"), HttpStatus.OK);
    }

    public ResponseEntity<MessageResponse> resetPassword(ResetPasswordRequest request) {
        customLog.info(CUSTOM_LOG_MARKER, "Resetting password");

        Integer userId;
        try {
            userId = tokenService.validateToken(request.token());
        } catch (Exception e) {
            customLog.warn(CUSTOM_LOG_MARKER, "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "User with ID {} not found", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("User not found"));
        }

        User user = userOpt.get();

        String hashedPassword = passwordEncoder.encode(request.newPassword());

        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        customLog.info(CUSTOM_LOG_MARKER, "Password successfully reset for user with email: {}", user.getEmail());
        return new ResponseEntity<>(new MessageResponse("Password successfully reset"), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<MessageResponse> confirmEmail(String token) {

        Integer userId;
        try {
            userId = tokenService.validateToken(token);
        } catch (Exception e) {
            customLog.warn(CUSTOM_LOG_MARKER, "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(new MessageResponse(e.getMessage()));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            customLog.warn(CUSTOM_LOG_MARKER, "User with ID {} not found", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(new MessageResponse("User not found"));
        }

        User user = userOpt.get();

        customLog.info(CUSTOM_LOG_MARKER, "Confirming email: {}", user.getEmail());

        user.setIsAuthenticated(true);
        userRepository.save(user);

        customLog.info(CUSTOM_LOG_MARKER, "Email confirmed successfully for {}", user.getEmail());
        return new ResponseEntity<>(new MessageResponse("Email successfully confirmed"), HttpStatus.OK);
    }

    public ResponseEntity<MessageResponse> resendConfirmation(ResendConfirmationRequest request) {

        customLog.info(CUSTOM_LOG_MARKER, "Resending confirmation link to user with email: {}", request.email());

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = tokenService.generateToken(user.getId(), TokenType.EMAIL_CONFIRMATION_TOKEN);
        String confirmationLink = "http://localhost:8010/api/v1/auth/email/confirm?token=" + token;

        Map<String, String> placeholders = Map.of(
                "confirmation_link", confirmationLink
        );

        kafkaProducer.sendMessageToAuthNotificationTopic(new EmailNotificationDto(
                user.getId(),
                EmailNotificationSubject.EMAIL_CONFIRMATION_NOTIFICATION,
                placeholders
        ));

        return new ResponseEntity<>(new MessageResponse("Success")
                , HttpStatus.OK);
    }
}
