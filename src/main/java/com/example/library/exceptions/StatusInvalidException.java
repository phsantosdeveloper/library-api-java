package com.example.library.exceptions;

public class StatusInvalidException extends RuntimeException {
    public StatusInvalidException(String message) {
        super(message);
    }
}
