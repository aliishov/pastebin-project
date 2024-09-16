package com.raul.paste_service.services;

import com.raul.paste_service.dto.PostDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaPostProducer {

    private final KafkaTemplate<String, PostDto> kafkaTemplate;

    public void sendMessage(PostDto postDto) {
        log.info("sending post to hash service");
        kafkaTemplate.send("hash_topic", postDto);
    }
}
