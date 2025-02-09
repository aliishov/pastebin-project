package com.raul.auth_service.service;

import com.raul.auth_service.model.Token;
import com.raul.auth_service.model.TokenType;
import com.raul.auth_service.repository.TokenRepository;
import com.raul.auth_service.utils.exceptions.TokenNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final TokenRepository tokenRepository;

    public String generateToken(Integer userId, TokenType tokenType) {
        log.info("Generating new token with type {}", tokenType.toString());

        String token = UUID.randomUUID().toString();

        Token tokenDomain = Token.builder()
                .token(token)
                .tokenType(tokenType)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .confirmedAt(null)
                .userId(userId)
                .build();

        tokenRepository.save(tokenDomain);
        log.info("Token generated successfully: {}", token);

        return token;
    }

    public Integer validateToken(String token) {
        var tokenDomain = tokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Token not found"));

        if (tokenDomain.getConfirmedAt() != null) {
            throw new IllegalArgumentException("Token already confirmed");
        }

        if (tokenDomain.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        tokenDomain.setConfirmedAt(LocalDateTime.now());

        return tokenDomain.getUserId();
    }
}
