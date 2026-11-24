package com.example.library.dtos;

import com.example.library.entities.Book;

public record BookMinDTO(
        Long id,
        String title,
        String isbn
) {

    public BookMinDTO(Book book) {
        this(book.getId(), book.getTitle(), book.getIsbn());
    }
}
