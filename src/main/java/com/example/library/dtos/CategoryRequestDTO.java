package com.example.library.dtos;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequestDTO(

        @NotBlank(message = "Name is required")
        String name
) {
}
