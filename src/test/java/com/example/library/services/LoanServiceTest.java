package com.example.library.services;

import com.example.library.dtos.LoanRequestDTO;
import com.example.library.dtos.LoanResponseDTO;
import com.example.library.dtos.LoanUpdateReturnDateDTO;
import com.example.library.entities.Author;
import com.example.library.entities.Book;
import com.example.library.entities.Category;
import com.example.library.entities.Loan;
import com.example.library.entities.User;
import com.example.library.entities.enums.LoanStatus;
import com.example.library.exceptions.ActiveLoanDelinquencyException;
import com.example.library.exceptions.BookUnavailableException;
import com.example.library.exceptions.ForbiddenException;
import com.example.library.exceptions.InvalidDateException;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.exceptions.StatusInvalidException;
import com.example.library.repositories.BookRepository;
import com.example.library.repositories.LoanRepository;
import com.example.library.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    private static final String NOT_FOUND_MESSAGE = "Resource not found, id: 99";

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        // @PersistenceContext is field-injected; Mockito stops after constructor injection, so it is set by hand
        ReflectionTestUtils.setField(loanService, "entityManager", entityManager);
    }

    private User user() {
        return new User(1L, "Maria Silva", "maria@gmail.com", "(11) 99999-0001", "123456");
    }

    private Book book(int availableCopies) {
        return new Book(1L, "Dom Casmurro", "9788535914849", LocalDate.of(1899, 1, 1), availableCopies,
                new Author(1L, "Machado de Assis"), new Category(1L, "Romance"));
    }

    private Loan loan(LoanStatus status, int availableCopies) {
        LocalDate loanDate = LocalDate.now().minusDays(5);
        return new Loan(1L, loanDate, loanDate.plusDays(30), null, status, user(), book(availableCopies));
    }

    // ---------- findAll ----------

    @Test
    void shouldReturnPaginationOfLoans() {

        //arrange
        Pageable pageable = PageRequest.of(0, 20);
        Loan loan = loan(LoanStatus.ACTIVE, 5);
        Loan loan1 = new Loan(2L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                LocalDateTime.of(2026, 10, 20, 14, 0, 0), LoanStatus.RETURNED, user(), book(5));

        when(loanRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(loan, loan1), pageable, 2));

        //act
        Page<LoanResponseDTO> result = loanService.findAll(pageable, null, null);

        //assert
        assertEquals(List.of(new LoanResponseDTO(loan), new LoanResponseDTO(loan1)), result.getContent());
        assertEquals(20, result.getSize());
        assertEquals(2L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        verify(loanRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnPaginationOfLoansWhenFiltersAreInformed() {

        //arrange
        Pageable pageable = PageRequest.of(0, 20);
        Loan loan = loan(LoanStatus.ACTIVE, 5);

        when(loanRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(loan), pageable, 1));

        //act
        Page<LoanResponseDTO> result = loanService.findAll(pageable, "maria@gmail.com", LoanStatus.ACTIVE);

        //assert
        assertEquals(1, result.getContent().size());
        assertEquals(new LoanResponseDTO(loan), result.getContent().get(0));
        verify(loanRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnEmptyPaginationOfLoans() {

        //arrange
        Pageable pageable = PageRequest.of(0, 20);

        when(loanRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        //act
        Page<LoanResponseDTO> result = loanService.findAll(pageable, null, null);

        //assert
        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getTotalElements());
    }

    // ---------- findById ----------

    @Test
    void shouldReturnLoanSearchedById() {

        //arrange
        Loan loan = loan(LoanStatus.ACTIVE, 5);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        //act
        LoanResponseDTO result = loanService.findById(1L);

        //assert
        assertEquals(new LoanResponseDTO(loan), result);
        assertEquals(1L, result.user().id());
        assertEquals(1L, result.book().id());
        verify(authService).validateSelfOrAdmin(1L);
    }

    @Test
    void shouldThrowExceptionWhenSearchingForNonExistentLoan() {

        //arrange
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.findById(99L)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());
        verifyNoInteractions(authService);
    }

    @Test
    void shouldThrowExceptionWhenUserSearchesForLoanOfAnotherUser() {

        //arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan(LoanStatus.ACTIVE, 5)));
        doThrow(new ForbiddenException("Access denied")).when(authService).validateSelfOrAdmin(1L);

        //act and assert
        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> loanService.findById(1L)
        );

        assertEquals("Access denied", exception.getMessage());
    }

    // ---------- findMyLoans ----------

    @Test
    void shouldReturnPaginationOfLoansOfAuthenticatedUser() {

        //arrange
        Pageable pageable = PageRequest.of(0, 20);
        Loan loan = loan(LoanStatus.ACTIVE, 5);

        when(userService.authenticated()).thenReturn(user());
        when(loanRepository.findByUserId(pageable, 1L)).thenReturn(new PageImpl<>(List.of(loan), pageable, 1));

        //act
        Page<LoanResponseDTO> result = loanService.findMyLoans(pageable);

        //assert
        assertEquals(List.of(new LoanResponseDTO(loan)), result.getContent());
        assertEquals(1L, result.getTotalElements());
        verify(loanRepository).findByUserId(pageable, 1L);
    }

    // ---------- insert ----------

    @Test
    void shouldInsertLoanWithValidData() {

        //arrange
        Book book = book(5);
        LocalDate today = LocalDate.now();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(false);
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(loanRepository.save(any(Loan.class))).thenAnswer(call -> {
            Loan loan = call.getArgument(0);
            loan.setId(10L);
            return loan;
        });

        //act
        LoanResponseDTO result = loanService.insert(new LoanRequestDTO(1L, 1L));
        ArgumentCaptor<Loan> captor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(captor.capture());
        Loan loanCaptured = captor.getValue();

        //assert
        assertEquals(10L, result.id());
        assertEquals(today, loanCaptured.getLoanDate());
        assertEquals(today.plusDays(30), loanCaptured.getExpectedReturnDate());
        assertNull(loanCaptured.getTimeReturn());
        assertEquals(LoanStatus.ACTIVE, loanCaptured.getStatus());
        assertEquals(1L, loanCaptured.getUser().getId());
        assertEquals(1L, loanCaptured.getBook().getId());
        assertEquals(4, book.getAvailableCopies());
        assertEquals(new LoanResponseDTO(loanCaptured), result);
    }

    @Test
    void shouldInsertLoanWhenOnlyOneCopyIsAvailable() {

        //arrange
        Book book = book(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(false);
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));
        when(loanRepository.save(any(Loan.class))).thenAnswer(call -> call.getArgument(0));

        //act
        loanService.insert(new LoanRequestDTO(1L, 1L));

        //assert
        assertEquals(0, book.getAvailableCopies());
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void shouldThrowExceptionWhenInsertingLoanWithNonExistentUser() {

        //arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.insert(new LoanRequestDTO(99L, 1L))
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());
        verifyNoInteractions(bookRepository);
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldThrowExceptionWhenInsertingLoanForUserWithOverdueLoans() {

        //arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(true);

        //act and assert
        ActiveLoanDelinquencyException exception = assertThrows(
                ActiveLoanDelinquencyException.class,
                () -> loanService.insert(new LoanRequestDTO(1L, 1L))
        );

        assertEquals("It is not possible to perform this operation with overdue loans", exception.getMessage());
        verifyNoInteractions(bookRepository);
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldCheckOverdueLoansUsingCurrentDate() {

        //arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(true);

        //act
        assertThrows(ActiveLoanDelinquencyException.class, () -> loanService.insert(new LoanRequestDTO(1L, 1L)));

        //assert
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(loanRepository).existsByExpectedReturnDateBeforeAndUserIdAndStatus(captor.capture(), eq(1L),
                eq(LoanStatus.ACTIVE));
        assertEquals(LocalDate.now(), captor.getValue());
    }

    @Test
    void shouldThrowExceptionWhenInsertingLoanWithNonExistentBook() {

        //arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(false);
        when(bookRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.insert(new LoanRequestDTO(1L, 99L))
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldThrowExceptionWhenInsertingLoanWithNoAvailableCopies() {

        //arrange
        Book book = book(0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(false);
        when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

        //act and assert
        BookUnavailableException exception = assertThrows(
                BookUnavailableException.class,
                () -> loanService.insert(new LoanRequestDTO(1L, 1L))
        );

        assertEquals("Insufficient number of copies for this operation", exception.getMessage());
        assertEquals(0, book.getAvailableCopies());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    // ---------- updateExpectedReturnDate ----------

    @Test
    void shouldUpdateExpectedReturnDateWithValidDate() {

        //arrange
        Loan loan = loan(LoanStatus.ACTIVE, 5);
        LocalDate newDate = loan.getExpectedReturnDate().plusDays(10);

        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(false);

        //act
        LoanResponseDTO result = loanService.updateExpectedReturnDate(1L, new LoanUpdateReturnDateDTO(newDate));

        //assert
        assertEquals(newDate, result.expectedReturnDate());
        assertEquals(newDate, loan.getExpectedReturnDate());
        assertEquals(LoanStatus.ACTIVE, result.status());
        verify(loanRepository).save(loan);
    }

    @Test
    void shouldUpdateExpectedReturnDateWhenDateIsExactlyAtTheLimit() {

        //arrange
        Loan loan = loan(LoanStatus.ACTIVE, 5);
        LocalDate limit = loan.getExpectedReturnDate().plusDays(30);

        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(false);

        //act
        LoanResponseDTO result = loanService.updateExpectedReturnDate(1L, new LoanUpdateReturnDateDTO(limit));

        //assert
        assertEquals(limit, result.expectedReturnDate());
        verify(loanRepository).save(loan);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingExpectedReturnDateBeyondTheLimit() {

        //arrange
        Loan loan = loan(LoanStatus.ACTIVE, 5);
        LocalDate originalDate = loan.getExpectedReturnDate();
        LocalDate beyondLimit = originalDate.plusDays(31);

        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(false);

        //act and assert
        InvalidDateException exception = assertThrows(
                InvalidDateException.class,
                () -> loanService.updateExpectedReturnDate(1L, new LoanUpdateReturnDateDTO(beyondLimit))
        );

        assertEquals("Impossible to update to this date", exception.getMessage());
        assertEquals(originalDate, loan.getExpectedReturnDate());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingExpectedReturnDateOfNonActiveLoan() {

        //arrange
        Loan loan = loan(LoanStatus.RETURNED, 5);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        //act and assert
        StatusInvalidException exception = assertThrows(
                StatusInvalidException.class,
                () -> loanService.updateExpectedReturnDate(1L,
                        new LoanUpdateReturnDateDTO(loan.getExpectedReturnDate().plusDays(5)))
        );

        assertEquals("Only active loans can be modified", exception.getMessage());
        verify(loanRepository, never()).existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(), anyLong(), any());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingExpectedReturnDateForUserWithOverdueLoans() {

        //arrange
        Loan loan = loan(LoanStatus.ACTIVE, 5);
        LocalDate originalDate = loan.getExpectedReturnDate();

        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.existsByExpectedReturnDateBeforeAndUserIdAndStatus(any(LocalDate.class), eq(1L),
                eq(LoanStatus.ACTIVE))).thenReturn(true);

        //act and assert
        assertThrows(
                ActiveLoanDelinquencyException.class,
                () -> loanService.updateExpectedReturnDate(1L, new LoanUpdateReturnDateDTO(originalDate.plusDays(5)))
        );

        assertEquals(originalDate, loan.getExpectedReturnDate());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingExpectedReturnDateOfNonExistentLoan() {

        //arrange
        when(loanRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.updateExpectedReturnDate(99L, new LoanUpdateReturnDateDTO(LocalDate.now().plusDays(5)))
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    // ---------- processReturn ----------

    @Test
    void shouldProcessReturnOfActiveLoan() {

        //arrange
        Loan loan = loan(LoanStatus.ACTIVE, 4);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        //act
        LocalDateTime before = LocalDateTime.now();
        LoanResponseDTO result = loanService.processReturn(1L);
        LocalDateTime after = LocalDateTime.now();

        //assert
        assertEquals(LoanStatus.RETURNED, result.status());
        assertNotNull(result.timeReturn());
        assertFalse(result.timeReturn().isBefore(before));
        assertFalse(result.timeReturn().isAfter(after));
        assertEquals(5, loan.getBook().getAvailableCopies());
        verify(entityManager).refresh(loan.getBook(), LockModeType.PESSIMISTIC_WRITE);
        verify(loanRepository).save(loan);
    }

    @Test
    void shouldLockBookBeforeReturningItToStock() {

        //arrange
        Loan loan = loan(LoanStatus.ACTIVE, 4);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));
        // simulates the refresh reloading a value updated by another transaction
        doAnswer(call -> {
            Book book = call.getArgument(0);
            book.setAvailableCopies(2);
            return null;
        }).when(entityManager).refresh(loan.getBook(), LockModeType.PESSIMISTIC_WRITE);

        //act
        loanService.processReturn(1L);

        //assert
        assertEquals(3, loan.getBook().getAvailableCopies());
    }

    @Test
    void shouldThrowExceptionWhenProcessingReturnOfNonActiveLoan() {

        //arrange
        Loan loan = loan(LoanStatus.CANCELED, 4);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        //act and assert
        StatusInvalidException exception = assertThrows(
                StatusInvalidException.class,
                () -> loanService.processReturn(1L)
        );

        assertEquals("Only active loans can be modified", exception.getMessage());
        assertEquals(LoanStatus.CANCELED, loan.getStatus());
        assertNull(loan.getTimeReturn());
        assertEquals(4, loan.getBook().getAvailableCopies());
        verifyNoInteractions(entityManager);
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldThrowExceptionWhenProcessingReturnOfNonExistentLoan() {

        //arrange
        when(loanRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.processReturn(99L)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());
        verifyNoInteractions(entityManager);
        verify(loanRepository, never()).save(any(Loan.class));
    }

    // ---------- cancel ----------

    @Test
    void shouldCancelActiveLoan() {

        //arrange
        Loan loan = loan(LoanStatus.ACTIVE, 4);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        //act
        LoanResponseDTO result = loanService.cancel(1L);

        //assert
        assertEquals(LoanStatus.CANCELED, result.status());
        assertNull(result.timeReturn());
        assertEquals(5, loan.getBook().getAvailableCopies());
        verify(entityManager).refresh(loan.getBook(), LockModeType.PESSIMISTIC_WRITE);
        verify(loanRepository).save(loan);
    }

    @Test
    void shouldThrowExceptionWhenCancelingNonActiveLoan() {

        //arrange
        Loan loan = loan(LoanStatus.RETURNED, 4);
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        //act and assert
        StatusInvalidException exception = assertThrows(
                StatusInvalidException.class,
                () -> loanService.cancel(1L)
        );

        assertEquals("Only active loans can be modified", exception.getMessage());
        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertEquals(4, loan.getBook().getAvailableCopies());
        verifyNoInteractions(entityManager);
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldThrowExceptionWhenCancelingNonExistentLoan() {

        //arrange
        when(loanRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        //act and assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> loanService.cancel(99L)
        );

        assertEquals(NOT_FOUND_MESSAGE, exception.getMessage());
        verifyNoInteractions(entityManager);
        verify(loanRepository, never()).save(any(Loan.class));
    }
}
