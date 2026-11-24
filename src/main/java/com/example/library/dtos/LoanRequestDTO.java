package com.example.library.dtos;

import jakarta.validation.constraints.NotNull;

public record LoanRequestDTO(

        @NotNull(message = "User id is required")
        Long userId,

        @NotNull(message = "Book id is required")
        Long bookId
) {
}
