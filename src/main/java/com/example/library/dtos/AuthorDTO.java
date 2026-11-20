package com.example.library.dtos;

import com.example.library.entities.Author;

public record AuthorDTO(
        Long id,
        String name
) {

    public AuthorDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public AuthorDTO(Author author) {
        this(author.getId(), author.getName());
    }
}
