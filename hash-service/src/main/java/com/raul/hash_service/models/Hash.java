package com.raul.hash_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "hashes")
@EntityListeners(AuditingEntityListener.class)
public class Hash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, name = "hash", unique = true)
    private String hash;

    @Column(nullable = false, name = "post_id")
    private Integer postId;

    @CreatedDate
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "is_deleted")
    private Boolean isDeleted;
}
