package com.proops2026.taskservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> handleForbidden(UnauthorizedException ex) {
        log.warn("Forbidden action: {}", ex.getMessage());
        return ResponseEntity.status(403).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("validation error");
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(400).body(new ErrorResponse(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body");
        return ResponseEntity.status(400).body(new ErrorResponse("request body is required"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(new ErrorResponse("internal server error"));
    }
}

