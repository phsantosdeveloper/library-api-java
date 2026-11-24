package com.example.library.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LoanUpdateReturnDateDTO(

        @NotNull(message = "Expected return date is required")
        @Future(message = "Invalid date")
        LocalDate expectedReturnDate
) {
}
