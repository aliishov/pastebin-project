package com.raul.auth_service.service;

import com.raul.auth_service.dto.AuthenticationRequest;
import com.raul.auth_service.dto.AuthenticationResponse;
import com.raul.auth_service.dto.RegisterRequest;
import com.raul.auth_service.model.Role;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public ResponseEntity<AuthenticationResponse> register(RegisterRequest request){
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

        var jwtToken = jwtService.generateToken(user);

        return new ResponseEntity<>(AuthenticationResponse
                .builder()
                .token(jwtToken)
                .build(), HttpStatus.CREATED);

    }

    public ResponseEntity<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User with email: " + request.getEmail() + " not found"));

        user.setIsAuthenticated(true);
        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);

        return new ResponseEntity<>(AuthenticationResponse
                .builder()
                .token(jwtToken)
                .build(), HttpStatus.OK);
    }
}
