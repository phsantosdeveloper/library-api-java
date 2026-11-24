package com.example.library.dtos;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.ISBN;

import java.time.LocalDate;

public record BookRequestDTO(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "ISBN is required")
        @ISBN(type = ISBN.Type.ANY, message = "Invalid ISBN")
        String isbn,

        @NotNull(message = "Publication date is required")
        @PastOrPresent(message = "Invalid date")
        LocalDate publicationDate,

        @NotNull(message = "Available copies is required")
        @PositiveOrZero(message = "Invalid number")
        Integer availableCopies,

        @NotNull(message = "Author is required")
        Long authorId,

        @NotNull(message = "Category is required")
        Long categoryId
) {
}
