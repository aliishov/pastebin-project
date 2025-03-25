package com.raul.paste_service.utils.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReviewNotFoundException extends RuntimeException{
    private final String msg;
}

