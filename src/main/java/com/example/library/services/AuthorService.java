package com.example.library.services;

import com.example.library.dtos.AuthorDTO;
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
    public Page<AuthorDTO> findAll(Pageable pageable) {
        Page<Author> authors = authorRepository.findAll(pageable);
        return authors.map(author -> new AuthorDTO(author));
    }

    @Transactional(readOnly = true)
    public AuthorDTO findById(Long id) {
        Author author = searchAuthor(id);
        return new AuthorDTO(author);
    }

    @Transactional
    public AuthorDTO insert(AuthorDTO dto) {
        Author author = new Author(null, dto.name());
        authorRepository.save(author);
        return new AuthorDTO(author);
    }

    @Transactional
    public AuthorDTO update(Long id, AuthorDTO dto) {
        Author author = searchAuthor(id);
        author.setName(dto.name());
        authorRepository.save(author);
        return new AuthorDTO(author);
    }

    @Transactional
    public void delete(Long id) {
        Author author = searchAuthor(id);
        authorRepository.delete(author);
    }

    private Author searchAuthor(Long id) {
        Author authorFound = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        return authorFound;
    }

}
