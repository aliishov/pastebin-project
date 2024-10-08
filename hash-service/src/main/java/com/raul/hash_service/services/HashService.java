package com.raul.hash_service.services;

import com.raul.hash_service.dto.PostDto;
import com.raul.hash_service.models.Hash;
import com.raul.hash_service.repositories.HashRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HashService {

    private final HashGenerationService hashGenerationService;
    private final HashRepository hashRepository;

    @KafkaListener(topics = "hash_topic", groupId = "${spring.kafka.consumer.group-id}")
    public void consumePostDto(PostDto postDto) {
        log.info("Received message from Kafka for post with id: {}", postDto.id());

        String hash = hashGenerationService.generateUniqueHash(postDto);

        log.info("Hash '{}' successfully generated and saved for post with id: {}", hash, postDto.id());
    }

    public ResponseEntity<Integer> getPostIdByHash(String hash) {
        log.info("Received request to find post ID by hash: {}", hash);

        Hash hashEntity = hashRepository.findByHash(hash)
                .orElseThrow(() -> {
                    log.error("Hash not found: {}", hash);
                    return new IllegalArgumentException("Hash not found");
                });

        log.info("Hash found for post ID: {}", hashEntity.getPostId());

        return new ResponseEntity<>(hashEntity.getPostId(), HttpStatus.OK);
    }

    public ResponseEntity<String> getHashByPostId(Integer postId) {
        log.info("Received request to find hash by post ID: {}", postId);

        Hash hashEntity = hashRepository.findByPostId(postId)
                .orElseThrow(() -> {
                    log.error("Hash not found");
                    return new IllegalArgumentException("Hash not found");
                });

        log.info("Hash found for post ID: {}", hashEntity.getPostId());

        return new ResponseEntity<>(hashEntity.getHash(), HttpStatus.OK);
    }
}
