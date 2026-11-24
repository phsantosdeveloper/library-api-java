package com.example.library.exceptions;

public class ActiveLoanDelinquencyException extends RuntimeException {
    public ActiveLoanDelinquencyException(String message) {
        super(message);
    }
}
