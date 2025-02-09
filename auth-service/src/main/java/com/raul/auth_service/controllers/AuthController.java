package com.raul.auth_service.controllers;

import com.raul.auth_service.dto.*;
import com.raul.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("register")
    public ResponseEntity<MessageResponse> register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid AuthenticationRequest request) {
        return authService.login(request);
    }

    @PostMapping("forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @GetMapping("confirm-email")
    public ResponseEntity<MessageResponse> confirmEmail(@RequestParam String token) {
        return authService.confirmEmail(token);
    }

//    @PostMapping("resend-confirmation")
//    public ResponseEntity<MessageResponse> resendConfirmation(@RequestBody @Valid ResendConfirmationRequest request) {
//        return authService.resendConfirmation(request);
//    }
}
