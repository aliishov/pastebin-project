package com.raul.paste_service.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic emailNotificationTopic() {
        return TopicBuilder
                .name("email_notification_topic")
                .build();
    }

    @Bean
    public NewTopic postIndexTopic() {
        return TopicBuilder
                .name("post_index_topic")
                .build();
    }
}
