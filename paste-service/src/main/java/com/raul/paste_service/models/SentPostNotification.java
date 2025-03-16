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

    @Column(nullable = false, name = "post_id")
    private Integer postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "notification_type")
    private EmailNotificationSubject notificationType;

    @Column(nullable = false, name = "send_at")
    private LocalDateTime sendAt;
}
