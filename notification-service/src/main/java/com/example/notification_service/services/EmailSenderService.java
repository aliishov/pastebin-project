package com.example.notification_service.services;

import com.example.notification_service.client.UserClient;
import com.example.notification_service.dto.EmailNotificationDto;
import com.example.notification_service.dto.EmailNotificationSubject;
import com.example.notification_service.utils.exceptions.InvalidEmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class EmailSenderService {

    private final JavaMailSender mailSender;
    private final UserClient userClient;

    @Value("$notification.mail.send.from")
    private String from;

    @KafkaListener(topics = "email_notification_topic", groupId = "${spring.kafka.consumer.group-id}")
    public void sendEmail(EmailNotificationDto emailNotificationDto) {

        log.info("received email notification for user with ID: {}", emailNotificationDto.to());

        String email = getUserEmail(emailNotificationDto.to());
        if (email == null || email.isBlank()) {
            log.error("Invalid email address for user ID: " + emailNotificationDto.to());
            throw new InvalidEmailException("Invalid Email");
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            messageHelper.setTo(email);
            messageHelper.setFrom(from);
            messageHelper.setSubject(emailNotificationDto.subject().toString());
            messageHelper.setText(setEmailText(emailNotificationDto.subject()), true);

            mailSender.send(mimeMessage);
            log.info("Email sent to: " + email);
        } catch (MessagingException e) {
            log.error("Error while sending email to " + email, e);
        }
    }

    private String getUserEmail(Integer to) {

        var user = userClient.getUserById(to).getBody();
        if (user == null || user.email() == null) {
            log.error("User not found for user ID: " + to);
            throw new IllegalArgumentException("User not found");
        }

        return user.email();
    }

    private String setEmailText(EmailNotificationSubject subject) {

        return switch (subject) {
            case ACCOUNT_CREATION_NOTIFICATION -> "Welcome, Your account successfully created";
            case EMAIL_CONFIRMATION_NOTIFICATION -> "Email confirmed";
            case POPULAR_POST_NOTIFICATION -> "Congratulations! Your post becomes popular";
            case POST_EXPIRATION_NOTIFICATION -> "Your post will soon be deactivated.";
        };
    }
}
