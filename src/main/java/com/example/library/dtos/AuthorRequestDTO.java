package com.example.library.dtos;

import jakarta.validation.constraints.NotBlank;

public record AuthorRequestDTO(

        @NotBlank(message = "Name is required")
        String name
) {
}
