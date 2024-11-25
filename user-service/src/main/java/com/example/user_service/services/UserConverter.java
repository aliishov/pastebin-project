package com.example.user_service.services;

import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserConverter {

    public UserResponseDto convertToUserResponseDto(User user) {
        return new UserResponseDto(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}
