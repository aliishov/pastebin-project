package com.example.user_service.services;

import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCleanUpService {
    private final UserRepository userRepository;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    @Scheduled(cron = "${task.cleanup.cron}")
    public void removeAllDeletedUsers() {
        customLog.info(CUSTOM_LOG_MARKER, "Starting scheduled task to check and remove deleted users.");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            List<User> oldDeletedUsers = userRepository.findUsersDeletedBefore(threshold);

            if (oldDeletedUsers.isEmpty()) {
                customLog.info(CUSTOM_LOG_MARKER, "No deleted user found for removal.");
                return;
            }

            customLog.info(CUSTOM_LOG_MARKER, "Found {} deleted users for removal.", oldDeletedUsers.size());

            userRepository.deleteAll(oldDeletedUsers);

            customLog.info(CUSTOM_LOG_MARKER, "Successfully remove {} deleted users.", oldDeletedUsers.size());
        } catch (Exception e) {
            customLog.error(CUSTOM_LOG_MARKER, "Error occurred during deleted users removal process.", e);
            throw new RuntimeException("Failed to clean up deleted users", e);
        }
    }
}
