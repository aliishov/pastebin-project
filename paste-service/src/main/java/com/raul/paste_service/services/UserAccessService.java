package com.raul.paste_service.services;

import jakarta.ws.rs.ForbiddenException;
import org.springframework.stereotype.Service;

@Service
public class UserAccessService {

    /**
     * Check if the user in path matches the one from header
     *
     * @param pathUserId    user ID from the path
     * @param headerUserId  user ID from the header
     */
    public void userAccessCheck(Integer pathUserId, String headerUserId) {
        if (!pathUserId.equals(Integer.parseInt(headerUserId))) {
            throw new ForbiddenException("Access denied");
        }
    }
}
