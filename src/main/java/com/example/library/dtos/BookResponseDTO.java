package com.example.library.dtos;

import com.example.library.entities.Book;

import java.time.LocalDate;

public record BookResponseDTO(
        Long id,
        String title,
        String isbn,
        LocalDate publicationDate,
        Integer totalCopies,
        AuthorResponseDTO author,
        CategoryResponseDTO category
) {

    public BookResponseDTO(Book book) {
        this(book.getId(), book.getTitle(), book.getIsbn(),
                book.getPublicationDate(), book.getTotalCopies(), new AuthorResponseDTO(book.getAuthor()),
                new CategoryResponseDTO(book.getCategory()));
    }
}
