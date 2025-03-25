package com.raul.hash_service.utils.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class HashNotFoundException extends RuntimeException{
    private final String msg;
}
