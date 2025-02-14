package com.example.user_service.services;

import com.example.user_service.model.Token;
import com.example.user_service.model.TokenType;
import com.example.user_service.repository.TokenRepository;
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
}
