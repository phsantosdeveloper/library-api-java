package com.example.library.dtos;

import com.example.library.entities.Author;

public record AuthorResponseDTO(

        Long id,
        String name
) {

    public AuthorResponseDTO(Author author) {
        this(author.getId(), author.getName());
    }
}
