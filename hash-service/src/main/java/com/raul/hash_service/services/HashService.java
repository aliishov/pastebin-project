package com.raul.hash_service.services;

import com.raul.hash_service.dto.PostDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HashService {

    private final HashGenerationService hashGenerationService;

    @KafkaListener(topics = "hash-topic")
    public void consumePostDto(PostDto postDto) {
        log.info("Received message from Kafka for post with id: {}", postDto.id());

        String hash = hashGenerationService.generateUniqueHash(postDto);

        log.info("Hash '{}' successfully generated and saved for post with id: {}", hash, postDto.id());
    }
}
