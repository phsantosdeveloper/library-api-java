package com.example.library.services;

import com.example.library.dtos.LoanRequestDTO;
import com.example.library.dtos.LoanResponseDTO;
import com.example.library.dtos.LoanUpdateReturnDateDTO;
import com.example.library.entities.Book;
import com.example.library.entities.Loan;
import com.example.library.entities.User;
import com.example.library.entities.enums.LoanStatus;
import com.example.library.exceptions.ActiveLoanDelinquencyException;
import com.example.library.exceptions.BookUnavailableException;
import com.example.library.exceptions.InvalidDateException;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.exceptions.StatusInvalidException;
import com.example.library.repositories.BookRepository;
import com.example.library.repositories.LoanRepository;
import com.example.library.repositories.UserRepository;
import com.example.library.specifications.LoanSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, UserRepository userRepository, AuthService authService, UserService userService) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public Page<LoanResponseDTO> findAll(Pageable pageable, String email, LoanStatus status) {
        Specification<Loan> spec = LoanSpecifications.filter(email, status);
        return loanRepository.findAll(spec, pageable).map(loan -> new LoanResponseDTO(loan));
    }

    @Transactional(readOnly = true)
    public LoanResponseDTO findById(Long id) {
        Loan loan = searchLoan(id);
        authService.validateSelfOrAdmin(loan.getUser().getId());
        return new LoanResponseDTO(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponseDTO> findMyLoans(Pageable pageable) {
        User me = userService.authenticated();
        Page<Loan> loans = loanRepository.findByUserId(pageable, me.getId());
        return loans.map(loan -> new LoanResponseDTO(loan));
    }

    @Transactional
    public LoanResponseDTO insert(LoanRequestDTO request) {
        LocalDate currentDate = LocalDate.now();
        LocalDate expectedReturnDate = currentDate.plusDays(30);
        LoanStatus status = LoanStatus.ACTIVE;
        User user = searchUser(request.userId());

        validateUserHasNoOverdueLoans(currentDate, user.getId(), LoanStatus.ACTIVE);

        Book book = searchBookForUpdate(request.bookId());
        validateQuantityOfAvailableCopies(book);

        Loan loan = new Loan(null, currentDate, expectedReturnDate, null, status, user, book);
        removeBookWhenProcessingLoan(book);

        loan = loanRepository.save(loan);
        return new LoanResponseDTO(loan);
    }

    @Transactional
    public LoanResponseDTO updateExpectedReturnDate(Long id, LoanUpdateReturnDateDTO dto) {
        LocalDate currentDate = LocalDate.now();

        Loan loan = searchLoanForUpdate(id);
        validateStatus(loan);
        validateUserHasNoOverdueLoans(currentDate, loan.getUser().getId(), LoanStatus.ACTIVE);

        if(dto.expectedReturnDate().isAfter(loan.getExpectedReturnDate().plusDays(30))) {
            throw new InvalidDateException("Impossible to update to this date");
        }

        loan.setExpectedReturnDate(dto.expectedReturnDate());
        loanRepository.save(loan);
        return new LoanResponseDTO(loan);
    }

    @Transactional
    public LoanResponseDTO processReturn(Long id) {
        Loan loan = searchLoanForUpdate(id);
        validateStatus(loan);
        lockBook(loan.getBook());

        loan.setTimeReturn(LocalDateTime.now());
        loan.setStatus(LoanStatus.RETURNED);
        loanRepository.save(loan);
        returnBookToStock(loan.getBook());

        return new LoanResponseDTO(loan);
    }

    @Transactional
    public LoanResponseDTO cancel(Long id) {
        Loan loan = searchLoanForUpdate(id);
        validateStatus(loan);
        lockBook(loan.getBook());
        loan.setStatus(LoanStatus.CANCELED);
        returnBookToStock(loan.getBook());

        loanRepository.save(loan);
        return new LoanResponseDTO(loan);
    }

    private Loan searchLoan(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    // Locks the loan row (SELECT ... FOR UPDATE) until the end of the transaction
    private Loan searchLoanForUpdate(Long id) {
        return loanRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    // Locks the book row (SELECT ... FOR UPDATE) until the end of the transaction
    private Book searchBookForUpdate(Long id) {
        return bookRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    // The book was already loaded along with the loan, without a lock. The refresh locks the row
    // and reloads the current state from the database, preventing the use of an outdated availableCopies value.
    private void lockBook(Book book) {
        entityManager.refresh(book, LockModeType.PESSIMISTIC_WRITE);
    }

    private User searchUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private void removeBookWhenProcessingLoan(Book book) {
        book.setAvailableCopies(book.getAvailableCopies() - 1);
    }

    private void returnBookToStock(Book book) {
        book.setAvailableCopies(book.getAvailableCopies() + 1);
    }

    private void validateQuantityOfAvailableCopies(Book book) {
        if(book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException("Insufficient number of copies for this operation");
        }
    }

    private void validateStatus(Loan loan) {
        if(loan.getStatus() != LoanStatus.ACTIVE) {
            throw new StatusInvalidException("Only active loans can be modified");
        }
    }

    private void validateUserHasNoOverdueLoans(LocalDate currentDate, Long id, LoanStatus status) {
        if(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(currentDate, id, status)) {
            throw new ActiveLoanDelinquencyException("It is not possible to perform this operation with overdue loans");
        }
    }
}
