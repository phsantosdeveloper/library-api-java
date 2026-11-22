package com.example.library.handler;

import com.example.library.dtos.CustomError;
import com.example.library.dtos.ValidationError;
import com.example.library.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CustomError> resourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {

        String error = "Resource not found";
        HttpStatus status = HttpStatus.NOT_FOUND;

        CustomError err = new CustomError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomError> methodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {

        String error = "Entity validation error";
        String message = "Error in data validation";
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

        ValidationError err = new ValidationError(Instant.now(), status.value(), error, message, request.getRequestURI());

        for(FieldError f : e.getBindingResult().getFieldErrors()) {
            err.addError(f.getField(), f.getDefaultMessage());
        }

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CustomError> dataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {

        String error = "Database integrity violated";
        String message = "The operation performed violates the integrity of the database";
        HttpStatus status = HttpStatus.CONFLICT;

        CustomError err = new CustomError(Instant.now(), status.value(), error, message, request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }
}
