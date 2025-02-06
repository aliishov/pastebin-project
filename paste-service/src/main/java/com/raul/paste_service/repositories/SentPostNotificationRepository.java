package com.raul.paste_service.repositories;

import com.raul.paste_service.models.SentPostNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SentPostNotificationRepository extends JpaRepository<SentPostNotification, Integer> {

    Optional<SentPostNotification> findByPostId(Integer postId);
}
