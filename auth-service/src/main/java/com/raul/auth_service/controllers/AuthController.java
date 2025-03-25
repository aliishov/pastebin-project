package com.raul.auth_service.controllers;

import com.raul.auth_service.dto.*;
import com.raul.auth_service.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth Controller", description = "Manage authentication & authorization in Auth Service")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Registers a new user and sends an email confirmation link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Authenticate user", description = "Logs in a user and returns a JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid AuthenticationRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Initiate password reset", description = "Sends a password reset link to the user's email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @Operation(summary = "Reset user password", description = "Resets the password using a token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password successfully reset"),
            @ApiResponse(responseCode = "400", description = "Invalid token or password format")
    })
    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @Operation(summary = "Confirm user email", description = "Confirms the user's email using a verification token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email confirmed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/email/confirm")
    public ResponseEntity<MessageResponse> confirmEmail(@RequestParam String token) {
        return authService.confirmEmail(token);
    }

    @Operation(summary = "Resend email confirmation", description = "Resends the email confirmation link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Confirmation email resent successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/email/resend-confirmation")
    public ResponseEntity<MessageResponse> resendConfirmation(@RequestBody @Valid ResendConfirmationRequest request) {
        return authService.resendConfirmation(request);
    }

    @Operation(summary = "Refresh JWT token", description = "Refreshing JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "404", description = "Invalid refresh token")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }
}
