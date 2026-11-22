package com.example.library.dtos;

import com.example.library.entities.Category;

public record CategoryResponseDTO(

        Long id,
        String name
) {

    public CategoryResponseDTO(Category category) {
        this(category.getId(), category.getName());
    }
}
