package com.example.library.dtos;

import com.example.library.entities.Book;

import java.time.LocalDate;

public record BookResponseDTO(
        Long id,
        String title,
        String isbn,
        LocalDate publicationDate,
        Integer availableCopies,
        AuthorResponseDTO author,
        CategoryResponseDTO category
) {

    public BookResponseDTO(Book book) {
        this(book.getId(), book.getTitle(), book.getIsbn(),
                book.getPublicationDate(), book.getAvailableCopies(), new AuthorResponseDTO(book.getAuthor()),
                new CategoryResponseDTO(book.getCategory()));
    }
}
