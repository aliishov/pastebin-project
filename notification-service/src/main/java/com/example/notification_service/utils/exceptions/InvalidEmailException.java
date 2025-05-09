package com.example.notification_service.utils.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InvalidEmailException extends RuntimeException{
    private final String msg;
}
