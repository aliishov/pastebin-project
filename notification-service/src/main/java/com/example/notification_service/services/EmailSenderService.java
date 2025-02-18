package com.example.notification_service.services;

import com.example.notification_service.client.UserClient;
import com.example.notification_service.dto.EmailNotificationDto;
import com.example.notification_service.dto.EmailNotificationSubject;
import com.example.notification_service.utils.exceptions.InvalidEmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class EmailSenderService {

    private final JavaMailSender mailSender;
    private final UserClient userClient;
    private final static Marker CUSTOM_LOG_MARKER = MarkerFactory.getMarker("CUSTOM_LOGGER");
    private static final Logger customLog = LoggerFactory.getLogger("CUSTOM_LOGGER");

    @Value("${notification.mail.send.from}")
    private String from;

    @KafkaListener(topics = "email_notification_topic", groupId = "${spring.kafka.consumer.group-id}")
    public void sendEmail(EmailNotificationDto emailNotificationDto) {

        customLog.info(CUSTOM_LOG_MARKER, "received email notification for user with ID: {}", emailNotificationDto.to());

        String email = getUserEmail(emailNotificationDto.to());
        if (email == null || email.isBlank()) {
            customLog.error(CUSTOM_LOG_MARKER, "Invalid email address for user ID: " + emailNotificationDto.to());
            throw new InvalidEmailException("Invalid Email");
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            messageHelper.setTo(email);
            messageHelper.setFrom(from);
            messageHelper.setSubject(emailNotificationDto.subject().toString());

            String emailContent = fillTemplatePlaceholders(getEmailTemplate(emailNotificationDto.subject()), emailNotificationDto.placeholders());
            messageHelper.setText(emailContent, true);

            mailSender.send(mimeMessage);
            customLog.info(CUSTOM_LOG_MARKER, "Email sent to: " + email);
        } catch (MessagingException e) {
            customLog.error(CUSTOM_LOG_MARKER, "Error while sending email to " + email, e);
        }
    }

    private String getUserEmail(Integer to) {

        var user = userClient.getUserById(to).getBody();
        if (user == null || user.email() == null) {
            customLog.error(CUSTOM_LOG_MARKER, "User not found for user ID: " + to);
            throw new IllegalArgumentException("User not found");
        }

        return user.email();
    }

    private String getEmailTemplate(EmailNotificationSubject subject) {
        try {
            String fileName = "emailTemplates/" + subject.name() + ".html";
            Path path = new ClassPathResource(fileName).getFile().toPath();
            return Files.readString(path);
        } catch (IOException e) {
            customLog.error(CUSTOM_LOG_MARKER, "Error loading email template for subject: " + subject, e);
            throw new RuntimeException("Email template not found");
        }
    }

    private String fillTemplatePlaceholders(String template, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }
}
