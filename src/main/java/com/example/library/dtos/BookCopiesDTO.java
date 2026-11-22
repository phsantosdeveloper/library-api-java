package com.example.library.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookCopiesDTO(

        @NotNull(message = "Quantity copies is required")
        @Positive(message = "Invalid number")
        Integer quantityCopies
) {
}
