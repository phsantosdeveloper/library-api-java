package com.example.library.services;

import com.example.library.dtos.AuthorResponseDTO;
import com.example.library.dtos.BookCopiesDTO;
import com.example.library.dtos.BookRequestDTO;
import com.example.library.dtos.BookResponseDTO;
import com.example.library.dtos.BookUpdateDTO;
import com.example.library.dtos.CategoryResponseDTO;
import com.example.library.entities.Author;
import com.example.library.entities.Book;
import com.example.library.entities.Category;
import com.example.library.exceptions.InvalidWithdrawalAmountException;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.mappers.BookMapper;
import com.example.library.mappers.BookMapperImpl;
import com.example.library.repositories.AuthorRepository;
import com.example.library.repositories.BookRepository;
import com.example.library.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    private static final String NOT_FOUND_MESSAGE = "Resource not found, id: 99";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private BookMapper bookMapper = new BookMapperImpl();

    @InjectMocks
    private BookService bookService;

    private Author author() {
        return new Author(1L, "Machado de Assis");
    }

    private Category category() {
        return new Category(1L, "Romance");
    }

    private Book book() {
        return new Book(1L, "Dom Casmurro", "9788535914849",
                LocalDate.of(1899, 1, 1), 10, author(), category());
    }

    // ---------- findAll ----------

    @Test
    void shouldReturnPaginationOfBooks() {

        //arrange
        Pageable pageable = PageRequest.of(0, 20);
        Book book = book();
        Book book1 = new Book(2L, "Effective Java", "9780134685991",
                LocalDate.of(2018, 1, 6), 5, new Author(2L, "Joshua Bloch"), new Category(2L, "Technology"));

        when(bookRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(book, book1), pageable, 2));

        //act
        Page<BookResponseDTO> result = bookService.findAll(null, null, null, null, pageable);

        //assert
        assertEquals(List.of(new BookResponseDTO(book), new BookResponseDTO(book1)), result.getContent());
        assertEquals(20, result.getSize());
        assertEquals(2L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        verify(bookRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnPaginationOfBooksWhenFiltersAreInformed() {

        //arrange
        Pageable pageable = PageRequest.of(0, 20);
        Book book = book();

        when(bookRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(book), pageable, 1));

        //act
        Page<BookResponseDTO> result = bookService.findAll("casmurro", "9788535914849",
                "machado", "romance", pageable);

        //assert
        assertEquals(1, result.getContent().size());
        assertEquals(new BookResponseDTO(book), result.getContent().get(0));
        verify(bookRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnEmptyPaginationOfBooks() {

        //arrange
        Pageable pageable = PageRequest.of(0, 20);

        when(bookRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        //act
        Page<BookResponseDTO> result = bookService.findAll(null, null, null, null, pageable);

        //assert
        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getTotalElements());
    }

    // ---------- findById ----------

    @Test
    void shouldReturnBookSearchedById() {

        //arrange
        Book book = book();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        //act
        BookResponseDTO result = bookService.findById(1L);

        //assert
        assertEquals(new BookResponseDTO(1L, "Dom Casmurro", "9788535914849", LocalDate.of(1899, 1, 1), 10,
                new AuthorResponseDTO(1L, "Machado de Assis"), new CategoryResponseDTO(1L, "Romance")), result);
        verify(bookRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenSearchingForNonExistentBook() {

        //arrange
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.findById(99L)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(bookRepository).findById(99L);
    }

    // ---------- insert ----------

    @Test
    void shouldInsertBookWithValidData() {

        //arrange
        BookRequestDTO request = new BookRequestDTO("Dom Casmurro", "9788535914849",
                LocalDate.of(1899, 1, 1), 10, 1L, 1L);

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category()));
        when(bookRepository.save(any(Book.class))).thenAnswer(call -> {
            Book book = call.getArgument(0);
            book.setId(1L);
            return book;
        });

        //act
        BookResponseDTO result = bookService.insert(request);
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book bookCaptured = captor.getValue();

        //assert
        assertEquals(new BookResponseDTO(1L, "Dom Casmurro", "9788535914849", LocalDate.of(1899, 1, 1), 10,
                new AuthorResponseDTO(1L, "Machado de Assis"), new CategoryResponseDTO(1L, "Romance")), result);
        assertEquals(1L, bookCaptured.getId());
        assertEquals("Dom Casmurro", bookCaptured.getTitle());
        assertEquals("9788535914849", bookCaptured.getIsbn());
        assertEquals(LocalDate.of(1899, 1, 1), bookCaptured.getPublicationDate());
        assertEquals(10, bookCaptured.getAvailableCopies());
        assertEquals(1L, bookCaptured.getAuthor().getId());
        assertEquals(1L, bookCaptured.getCategory().getId());
    }

    @Test
    void shouldThrowExceptionWhenInsertingBookWithNonExistentAuthor() {

        //arrange
        BookRequestDTO request = new BookRequestDTO("Dom Casmurro", "9788535914849",
                LocalDate.of(1899, 1, 1), 10, 99L, 1L);

        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.insert(request)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(authorRepository).findById(99L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void shouldThrowExceptionWhenInsertingBookWithNonExistentCategory() {

        //arrange
        BookRequestDTO request = new BookRequestDTO("Dom Casmurro", "9788535914849",
                LocalDate.of(1899, 1, 1), 10, 1L, 99L);

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author()));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.insert(request)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(categoryRepository).findById(99L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    // ---------- update ----------

    @Test
    void shouldUpdateBookWithValidData() {

        //arrange
        Book book = book();
        Author newAuthor = new Author(2L, "José de Alencar");
        Category newCategory = new Category(2L, "Classic");
        BookUpdateDTO request = new BookUpdateDTO("Iracema", "9780134685991",
                LocalDate.of(1865, 1, 1), 2L, 2L);

        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(authorRepository.findById(2L)).thenReturn(Optional.of(newAuthor));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));

        //act
        BookResponseDTO result = bookService.update(1L, request);

        //assert
        assertEquals(new BookResponseDTO(1L, "Iracema", "9780134685991", LocalDate.of(1865, 1, 1), 10,
                new AuthorResponseDTO(2L, "José de Alencar"), new CategoryResponseDTO(2L, "Classic")), result);
        verify(bookRepository).save(book);
    }

    @Test
    void shouldNotChangeAvailableCopiesWhenUpdatingBook() {

        //arrange
        Book book = book();
        BookUpdateDTO request = new BookUpdateDTO("Dom Casmurro", "9788535914849",
                LocalDate.of(1899, 1, 1), 1L, 1L);

        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category()));

        //act
        BookResponseDTO result = bookService.update(1L, request);

        //assert
        assertEquals(10, result.availableCopies());
        assertEquals(10, book.getAvailableCopies());
        assertEquals(1L, book.getId());
    }

    @Test
    void shouldThrowExceptionWhenUpdateForNonExistentBook() {

        //arrange
        BookUpdateDTO request = new BookUpdateDTO("Iracema", "9780134685991",
                LocalDate.of(1865, 1, 1), 1L, 1L);

        when(bookRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.update(99L, request)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(bookRepository).findByIdForUpdate(99L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingBookWithNonExistentAuthor() {

        //arrange
        BookUpdateDTO request = new BookUpdateDTO("Iracema", "9780134685991",
                LocalDate.of(1865, 1, 1), 99L, 1L);

        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book()));
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.update(1L, request)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(authorRepository).findById(99L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingBookWithNonExistentCategory() {

        //arrange
        BookUpdateDTO request = new BookUpdateDTO("Iracema", "9780134685991",
                LocalDate.of(1865, 1, 1), 1L, 99L);

        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book()));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author()));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.update(1L, request)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(categoryRepository).findById(99L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    // ---------- delete ----------

    @Test
    void shouldDeleteExistingBook() {

        //arrange
        Book book = book();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        //act
        bookService.delete(1L);

        //assert
        verify(bookRepository).delete(book);
    }

    @Test
    void shouldThrowExceptionOnDeleteNonExistentBook() {

        //arrange
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.delete(99L)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(bookRepository).findById(99L);
        verify(bookRepository, never()).delete(any(Book.class));
    }

    // ---------- addBookCopies ----------

    @Test
    void shouldAddBookCopies() {

        //arrange
        Book book = book();
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

        //act
        BookResponseDTO result = bookService.addBookCopies(1L, new BookCopiesDTO(5));

        //assert
        assertEquals(15, result.availableCopies());
        assertEquals(15, book.getAvailableCopies());
    }

    @Test
    void shouldThrowExceptionWhenAddingCopiesToNonExistentBook() {

        //arrange
        when(bookRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.addBookCopies(99L, new BookCopiesDTO(5))
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(bookRepository).findByIdForUpdate(99L);
    }

    // ---------- removeBookCopies ----------

    @Test
    void shouldRemoveBookCopies() {

        //arrange
        Book book = book();
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

        //act
        BookResponseDTO result = bookService.removeBookCopies(1L, new BookCopiesDTO(4));

        //assert
        assertEquals(6, result.availableCopies());
        assertEquals(6, book.getAvailableCopies());
    }

    @Test
    void shouldRemoveAllBookCopiesWhenQuantityEqualsStock() {

        //arrange
        Book book = book();
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

        //act
        BookResponseDTO result = bookService.removeBookCopies(1L, new BookCopiesDTO(10));

        //assert
        assertEquals(0, result.availableCopies());
        assertEquals(0, book.getAvailableCopies());
    }

    @Test
    void shouldThrowExceptionWhenRemovingMoreCopiesThanInStock() {

        //arrange
        Book book = book();
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

        //act and assert
        InvalidWithdrawalAmountException exception = assertThrows(
                InvalidWithdrawalAmountException.class,
                () -> bookService.removeBookCopies(1L, new BookCopiesDTO(11))
        );

        assertEquals("It is not possible to remove a quantity greater than what is currently in stock",
                exception.getMessage());
        assertEquals(10, book.getAvailableCopies());
    }

    @Test
    void shouldThrowExceptionWhenRemovingCopiesFromNonExistentBook() {

        //arrange
        when(bookRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.removeBookCopies(99L, new BookCopiesDTO(1))
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());

        verify(bookRepository).findByIdForUpdate(99L);
    }
}
