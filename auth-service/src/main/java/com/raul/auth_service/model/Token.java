package com.raul.auth_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, name = "token")
    private String token;

    @Column(nullable = false, name = "token_type")
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiredAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(nullable = false, name = "user_id")
    private Integer userId;

}
