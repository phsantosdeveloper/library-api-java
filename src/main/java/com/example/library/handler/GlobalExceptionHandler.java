package com.example.library.handler;

import com.example.library.dtos.CustomError;
import com.example.library.dtos.ValidationError;
import com.example.library.exceptions.*;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
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

    // As exceções do Spring vêm dos repositórios; as do jakarta.persistence vêm de chamadas diretas
    // ao EntityManager (ex.: refresh com lock no LoanService), que não passam pela tradução do Spring
    @ExceptionHandler({PessimisticLockingFailureException.class, LockTimeoutException.class, PessimisticLockException.class})
    public ResponseEntity<CustomError> pessimisticLockingFailure(RuntimeException e, HttpServletRequest request) {

        String error = "Resource locked";
        String message = "The resource is being modified by another request, please try again";
        HttpStatus status = HttpStatus.CONFLICT;

        CustomError err = new CustomError(Instant.now(), status.value(), error, message, request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidWithdrawalAmountException.class)
    public ResponseEntity<CustomError> invalidWithdrawalAmount(InvalidWithdrawalAmountException e, HttpServletRequest request) {

        String error = "Invalid withdrawal amount";
        String message = e.getMessage();
        HttpStatus status = HttpStatus.CONFLICT;

        CustomError err = new CustomError(Instant.now(), status.value(), error, message, request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(BookUnavailableException.class)
    public ResponseEntity<CustomError> bookUnavailable(BookUnavailableException e, HttpServletRequest request) {

        String error = "Book unavailable";
        HttpStatus status = HttpStatus.CONFLICT;

        CustomError err = new CustomError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(StatusInvalidException.class)
    public ResponseEntity<CustomError> statusInvalid(StatusInvalidException e, HttpServletRequest request) {

        String error = "Status invalid";
        HttpStatus status = HttpStatus.CONFLICT;

        CustomError err = new CustomError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ActiveLoanDelinquencyException.class)
    public ResponseEntity<CustomError> activeLoanDelinquency(ActiveLoanDelinquencyException e, HttpServletRequest request) {

        String error = "Active loan delinquency";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        CustomError err = new CustomError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<CustomError> forbiddenException(ForbiddenException e, HttpServletRequest request) {

        String error = "Forbidden";
        HttpStatus status = HttpStatus.FORBIDDEN;

        CustomError err = new CustomError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidDateException.class)
    public ResponseEntity<CustomError> invalidDate(InvalidDateException e, HttpServletRequest request) {

        String error = "Invalid date";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        CustomError err = new CustomError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(err);
    }
}
