package com.example.library.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.hibernate.validator.constraints.ISBN;

import java.time.LocalDate;

public record BookUpdateDTO(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "ISBN is required")
        @ISBN(type = ISBN.Type.ANY, message = "Invalid ISBN")
        String isbn,

        @NotNull(message = "Publication date is required")
        @PastOrPresent(message = "Invalid date")
        LocalDate publicationDate,

        @NotNull(message = "Author is required")
        Long authorId,

        @NotNull(message = "Category is required")
        Long categoryId
) {
}
