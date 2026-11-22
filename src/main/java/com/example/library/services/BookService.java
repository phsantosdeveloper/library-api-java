package com.example.library.services;

import com.example.library.dtos.BookCopiesDTO;
import com.example.library.dtos.BookRequestDTO;
import com.example.library.dtos.BookResponseDTO;
import com.example.library.dtos.BookUpdateDTO;
import com.example.library.entities.Book;
import com.example.library.exceptions.InvalidWithdrawalAmountException;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.mappers.BookMapper;
import com.example.library.repositories.AuthorRepository;
import com.example.library.repositories.BookRepository;
import com.example.library.repositories.CategoryRepository;
import com.example.library.specifications.BookSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository,
                       CategoryRepository categoryRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.bookMapper = bookMapper;
    }

    @Transactional(readOnly = true)
    public Page<BookResponseDTO> findAll(String title, String isbn, String author,
                                         String category, Pageable pageable) {
        Specification<Book> spec = BookSpecifications.filter(title, isbn, author, category);
        return bookRepository.findAll(spec, pageable).map(book -> new BookResponseDTO(book));
    }

    @Transactional(readOnly = true)
    public BookResponseDTO findById(Long id) {
        Book book = searchBook(id);
        return new BookResponseDTO(book);
    }

    @Transactional
    public BookResponseDTO insert(BookRequestDTO request) {
        Book book = bookMapper.toEntity(request);
        setRelations(book, request.authorId(), request.categoryId());
        bookRepository.save(book);
        return new BookResponseDTO(book);
    }

    @Transactional
    public BookResponseDTO update(Long id, BookUpdateDTO request) {
        Book book = searchBook(id);
        bookMapper.updateEntity(request, book);
        setRelations(book, request.authorId(), request.categoryId());
        bookRepository.save(book);
        return new BookResponseDTO(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = searchBook(id);
        bookRepository.delete(book);
    }

    @Transactional
    public BookResponseDTO addBookCopies(Long id, BookCopiesDTO dto) {
        Book book = searchBook(id);
        book.setTotalCopies(book.getTotalCopies() + dto.quantityCopies());
        return new BookResponseDTO(book);
    }

    @Transactional
    public BookResponseDTO removeBookCopies(Long id, BookCopiesDTO dto) {
        Book book = searchBook(id);
        if(dto.quantityCopies() > book.getTotalCopies()) {
            throw new InvalidWithdrawalAmountException("It is not possible to remove a quantity greater than what is currently in stock");
        }
        book.setTotalCopies(book.getTotalCopies() - dto.quantityCopies());
        return new BookResponseDTO(book);
    }

    private void setRelations(Book book, Long authorId, Long categoryId) {
        book.setAuthor(authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException(authorId)));
        book.setCategory(categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(categoryId)));
    }

    private Book searchBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

}
