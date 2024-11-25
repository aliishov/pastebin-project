package com.example.user_service.services;

import com.example.user_service.dto.UserResponseDto;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.utils.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserConverter userConverter;
    public ResponseEntity<UserResponseDto> getUserById(Integer id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User whit this ID not found"));

        return new ResponseEntity<>(userConverter.convertToUserResponseDto(user), HttpStatus.OK);
    }
}
