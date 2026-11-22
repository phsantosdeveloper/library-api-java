package com.example.library.controllers;

import com.example.library.dtos.BookCopiesDTO;
import com.example.library.dtos.BookRequestDTO;
import com.example.library.dtos.BookResponseDTO;
import com.example.library.dtos.BookUpdateDTO;
import com.example.library.services.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(value = "/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<Page<BookResponseDTO>> findAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        Page<BookResponseDTO> dtos = bookService.findAll(title, isbn, author, category, pageable);
        return ResponseEntity.ok().body(dtos);
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<BookResponseDTO> findById(@PathVariable Long id) {
        BookResponseDTO dto = bookService.findById(id);
        return ResponseEntity.ok().body(dto);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookResponseDTO> insert(@Valid @RequestBody BookRequestDTO request) {
        BookResponseDTO dto = bookService.insert(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @PutMapping(value = "/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookResponseDTO> update(@PathVariable Long id,
                                                  @Valid @RequestBody BookUpdateDTO request) {
        BookResponseDTO dto = bookService.update(id, request);
        return ResponseEntity.ok().body(dto);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/copies/add")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookResponseDTO> addCopies(@PathVariable Long id,
                                                     @Valid @RequestBody BookCopiesDTO request) {
        BookResponseDTO dto = bookService.addBookCopies(id, request);
        return ResponseEntity.ok().body(dto);
    }

    @PatchMapping(value = "/{id}/copies/remove")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BookResponseDTO> removeCopies(@PathVariable Long id,
                                                        @Valid @RequestBody BookCopiesDTO request) {
        BookResponseDTO dto = bookService.removeBookCopies(id, request);
        return ResponseEntity.ok().body(dto);
    }
}
