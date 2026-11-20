package com.example.library.services;

import com.example.library.dtos.AuthorRequestDTO;
import com.example.library.dtos.AuthorResponseDTO;
import com.example.library.entities.Author;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.repositories.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void shouldReturnPaginationOfAuthors() {

        //arrange
        var pageable = PageRequest.of(0, 20);
        var authors = List.of(
                new Author(1L, "Machado de Assis"),
                new Author(2L, "Jorge Amado")
        );

        when(authorRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(authors, pageable, 2));


        //act
        Page<AuthorResponseDTO> result = authorService.findAll(pageable);


        //assert
        assertEquals(List.of(
                new AuthorResponseDTO(1L, "Machado de Assis"),
                new AuthorResponseDTO(2L, "Jorge Amado")
        ), result.getContent());

        assertEquals(20, result.getSize());
        assertEquals(2L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
    }

    @Test
    void mustReturnAuthorSearchedById() {

        //arrange
        Author author = new Author(1L, "Machado de Assis");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        //act
        AuthorResponseDTO resultDto = authorService.findById(1L);

        //assert
        assertEquals(new AuthorResponseDTO(1L, "Machado de Assis"), resultDto);
        verify(authorRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenSearchingForNonExistentAuthor() {

        //arrange
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authorService.findById(99L)
        );

        assertEquals("Resource not found, id: 99", exception.getMessage());

        verify(authorRepository).findById(99L);
    }

    @Test
    void shouldInsertAuthorWithValidData() {

        //arrage
        AuthorRequestDTO request = new AuthorRequestDTO("Machado de Assis");

        when(authorRepository.save(any(Author.class))).thenAnswer(call -> {
            Author author = call.getArgument(0);
            author.setId(1L);
            return author;
        });

        //act
        AuthorResponseDTO result = authorService.insert(request);
        ArgumentCaptor<Author> captor = ArgumentCaptor.forClass(Author.class);
        verify(authorRepository).save(captor.capture());
        Author authorCaptured = captor.getValue();

        //assert
        assertEquals(new AuthorResponseDTO(1L, "Machado de Assis"), result);
        assertEquals(authorCaptured.getId(), 1L);
        assertEquals(authorCaptured.getName(), "Machado de Assis");
    }

    @Test
    void shouldUpdateAuthorWithValidData() {

        //arrage
        Author author = new Author(1L, "Joaquim Maria Machado de Assis");

        AuthorRequestDTO request = new AuthorRequestDTO("Machado de Assis");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        //act
        AuthorResponseDTO result = authorService.update(1L, request);

        //assert
        assertEquals(new AuthorResponseDTO(1L, "Machado de Assis"), result);

        verify(authorRepository).save(author);
    }

    @Test
    void shouldThrowExceptionWhenUpdateForNonExistentAuthor() {

        //arrange
        AuthorRequestDTO request = new AuthorRequestDTO("Machado de Assis");

        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authorService.update(99L, request)
        );

        assertEquals("Resource not found, id: 99", exception.getMessage());

        verify(authorRepository).findById(99L);
    }

    @Test
    void shouldDeleteExistingAuthor() {

        //arrange
        Author author = new Author(1L, "Joaquim Maria Machado de Assis");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        //act
        authorService.delete(1L);

        //assert
        verify(authorRepository).delete(author);
    }

    @Test
    void mustThrowExceptionOnDeleteAuthorNonexistent() {

        //arrange
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        //act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authorService.delete(99L)
        );

        assertEquals("Resource not found, id: 99", exception.getMessage());

        //assert
        verify(authorRepository).findById(99L);
    }

}
