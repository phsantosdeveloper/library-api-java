package com.example.library.entities;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150, nullable = false)
    private String title;

    @Column(length = 20,unique = true, nullable = false)
    private String isbn;

    @Column(name = "publication_date", nullable = false)
    private LocalDate publicationDate;

    @Column(name = "available_copies", nullable = false,
            check = @CheckConstraint(name = "ck_book_available_copies", constraint = "available_copies >= 0"))
    private Integer availableCopies;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "book")
    private List<Loan> loans = new ArrayList<>();

    public Book() {
    }

    public Book(Long id, String title, String isbn, LocalDate publicationDate, Integer availableCopies, Author author, Category category) {
        this.id = id;
        this.title = title;
        this.isbn = isbn;
        this.publicationDate = publicationDate;
        this.availableCopies = availableCopies;
        this.author = author;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Integer getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(Integer availableCopies) {
        this.availableCopies = availableCopies;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<Loan> getLoans() {
        return loans;
    }
}
