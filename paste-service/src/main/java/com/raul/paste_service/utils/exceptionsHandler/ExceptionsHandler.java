package com.raul.paste_service.utils.exceptionsHandler;

import com.raul.paste_service.utils.exceptions.PostNotFoundException;
import com.raul.paste_service.utils.exceptions.ReviewNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.ForbiddenException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class ExceptionsHandler {
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<String> handle(PostNotFoundException e) {
        return ResponseEntity.status(NOT_FOUND).body(e.getMsg());
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<String> handle(ReviewNotFoundException e) {
        return ResponseEntity.status(NOT_FOUND).body(e.getMsg());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handle(ForbiddenException e) {
        return ResponseEntity.status(FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handle(EntityNotFoundException e) {
        return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        var errors = new HashMap<String, String>();

        e.getBindingResult().getAllErrors()
                .forEach(error -> {
                    var fieldName = ((FieldError)error).getField();
                    var errorMessage = error.getDefaultMessage();
                    errors.put(fieldName, errorMessage);
                });

        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception e) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }
}
