package com.example.user_service.utils.exceptionsHandler;

import com.example.user_service.utils.exceptions.InvalidPasswordException;
import com.example.user_service.utils.exceptions.UserNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class ExceptionsHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handle(UserNotFoundException e) {
        return ResponseEntity.status(NOT_FOUND).body(e.getMsg());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<String> handle(InvalidPasswordException e) {
        return ResponseEntity.status(NOT_FOUND).body(e.getMsg());
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
