package com.raul.auth_service.controllers.utils.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TokenNotFoundException extends RuntimeException{
    private final String msg;
}
