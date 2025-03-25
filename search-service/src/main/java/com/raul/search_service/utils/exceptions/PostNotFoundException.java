package com.raul.search_service.utils.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PostNotFoundException extends RuntimeException{
    private final String msg;
}
