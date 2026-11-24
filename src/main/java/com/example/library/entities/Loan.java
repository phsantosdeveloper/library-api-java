package com.example.library.entities;

import com.example.library.entities.enums.LoanStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "expected_return_date", nullable = false)
    private LocalDate expectedReturnDate;

    @Column(name = "time_return")
    private LocalDateTime timeReturn;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    public Loan() {
    }

    public Loan(Long id, LocalDate loanDate, LocalDate expectedReturnDate, LocalDateTime timeReturn, LoanStatus status, User user, Book book) {
        this.id = id;
        this.loanDate = loanDate;
        this.expectedReturnDate = expectedReturnDate;
        this.timeReturn = timeReturn;
        this.status = status;
        this.user = user;
        this.book = book;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(LocalDate expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public LocalDateTime getTimeReturn() {
        return timeReturn;
    }

    public void setTimeReturn(LocalDateTime timeReturn) {
        this.timeReturn = timeReturn;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }
}
