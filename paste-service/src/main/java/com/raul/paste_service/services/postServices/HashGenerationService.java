package com.raul.paste_service.services.postServices;

import com.raul.paste_service.repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class HashGenerationService {

    private final PostRepository postRepository;
    private static final int HASH_LENGTH = 8;
    private final SecureRandom sRandom = new SecureRandom();

    /**
     * Generation unique hash for paste
     *
     * @return Generated hash.
     */
    public String generateUniqueHash() {
        String hash;
        do {
            hash = generateRandomHash();
        } while (hashExists(hash));

        return hash;
    }

    /**
     * Generate a random hash using Base64.
     *
     * @return Random hash.
     */
    private String generateRandomHash() {
        byte[] randomBytes = new byte[HASH_LENGTH];
        sRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Check if such hash already exists in the database.
     *
     * @param hash Hash to check.
     * @return true if hash exists, otherwise false.
     */
    private boolean hashExists(String hash) {
        return postRepository.existsByHash(hash);
    }
}
