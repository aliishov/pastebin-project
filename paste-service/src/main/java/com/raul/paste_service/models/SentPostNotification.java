package com.raul.paste_service.models;

import com.raul.paste_service.dto.notification.EmailNotificationSubject;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "sent_notification")
public class SentPostNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer postId;

    @Enumerated(EnumType.STRING)
    private EmailNotificationSubject notificationType;

    private LocalDateTime sendAt;
}
