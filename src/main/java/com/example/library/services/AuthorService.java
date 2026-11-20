package com.example.library.services;

import com.example.library.dtos.AuthorRequestDTO;
import com.example.library.dtos.AuthorResponseDTO;
import com.example.library.entities.Author;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.repositories.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuthorResponseDTO> findAll(Pageable pageable) {
        Page<Author> authors = authorRepository.findAll(pageable);
        return authors.map(author -> new AuthorResponseDTO(author));
    }

    @Transactional(readOnly = true)
    public AuthorResponseDTO findById(Long id) {
        Author author = searchAuthor(id);
        return new AuthorResponseDTO(author);
    }

    @Transactional
    public AuthorResponseDTO insert(AuthorRequestDTO request) {
        Author author = new Author(null, request.name());
        authorRepository.save(author);
        return new AuthorResponseDTO(author);
    }

    @Transactional
    public AuthorResponseDTO update(Long id, AuthorRequestDTO request) {
        Author author = searchAuthor(id);
        author.setName(request.name());
        authorRepository.save(author);
        return new AuthorResponseDTO(author);
    }

    @Transactional
    public void delete(Long id) {
        Author author = searchAuthor(id);
        authorRepository.delete(author);
    }

    private Author searchAuthor(Long id) {
        Author authorFound = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));

        return authorFound;
    }

}
