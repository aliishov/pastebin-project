package com.example.user_service.utils.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InvalidPasswordException extends RuntimeException {
    private final String msg;
}
