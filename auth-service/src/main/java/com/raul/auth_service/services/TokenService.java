package com.raul.auth_service.services;

import com.raul.auth_service.models.Token;
import com.raul.auth_service.models.TokenType;
import com.raul.auth_service.repositories.TokenRepository;
import com.raul.auth_service.utils.exceptions.TokenNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    public String generateToken(Integer userId, TokenType tokenType) {
        customLog.info(CUSTOM_LOG_MARKER, "Generating new token with type {}", tokenType.toString());

        String token = UUID.randomUUID().toString();

        Token tokenDomain = Token.builder()
                .token(token)
                .tokenType(tokenType)
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .confirmedAt(null)
                .userId(userId)
                .build();

        tokenRepository.save(tokenDomain);
        customLog.info(CUSTOM_LOG_MARKER, "Token generated successfully: {}", token);

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
