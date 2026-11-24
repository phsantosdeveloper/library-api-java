package com.example.library.exceptions;

public class InvalidDateException extends RuntimeException {
    public InvalidDateException(String message) {
        super(message);
    }
}
