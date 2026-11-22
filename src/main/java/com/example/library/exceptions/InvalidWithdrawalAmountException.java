package com.example.library.exceptions;

public class InvalidWithdrawalAmountException extends RuntimeException {
    public InvalidWithdrawalAmountException(String message) {
        super(message);
    }
}
