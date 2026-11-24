package com.example.library.services;

import com.example.library.dtos.LoanRequestDTO;
import com.example.library.exceptions.BookUnavailableException;
import com.example.library.exceptions.StatusInvalidException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração (sem @Transactional): cada thread abre a sua própria transação,
 * exatamente como requisições HTTP concorrentes fariam.
 */
@SpringBootTest
@ActiveProfiles("test")
class LoanServiceConcurrencyTest {

    private static final int THREADS = 10;
    private static final long BOOK_ID = 2L;
    private static final String TEST_EMAIL = "concurrency.test@example.com";

    @Autowired
    private LoanService loanService;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer originalCopies;
    private Long userId;

    @BeforeEach
    void setUp() {
        originalCopies = jdbc.queryForObject("SELECT available_copies FROM tb_book WHERE id = ?", Integer.class, BOOK_ID);
        jdbc.update("UPDATE tb_book SET available_copies = 1 WHERE id = ?", BOOK_ID);

        // usuário sem empréstimos, para que a validação de atraso nunca interfira no teste
        jdbc.update("INSERT INTO tb_user (name, email, phone, password) VALUES ('Concurrency Test', ?, '(11) 90000-9999', 'x')", TEST_EMAIL);
        userId = jdbc.queryForObject("SELECT id FROM tb_user WHERE email = ?", Long.class, TEST_EMAIL);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM tb_loan WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM tb_user WHERE id = ?", userId);
        jdbc.update("UPDATE tb_book SET available_copies = ? WHERE id = ?", originalCopies, BOOK_ID);
    }

    @Test
    void insertShouldCreateOnlyOneLoanWhenConcurrentRequestsDisputeTheLastCopy() throws Exception {
        List<Throwable> failures = runConcurrently(() -> loanService.insert(new LoanRequestDTO(userId, BOOK_ID)));

        assertThat(failures).hasSize(THREADS - 1).allMatch(e -> e instanceof BookUnavailableException);
        assertThat(countLoans()).isEqualTo(1);
        assertThat(availableCopies()).isZero();
    }

    @Test
    void processReturnShouldReturnTheBookToStockOnlyOnceWhenCalledConcurrently() throws Exception {
        Long loanId = loanService.insert(new LoanRequestDTO(userId, BOOK_ID)).id();
        assertThat(availableCopies()).isZero();

        List<Throwable> failures = runConcurrently(() -> loanService.processReturn(loanId));

        assertThat(failures).hasSize(THREADS - 1).allMatch(e -> e instanceof StatusInvalidException);
        assertThat(availableCopies()).isEqualTo(1);
    }

    @Test
    void cancelShouldReturnTheBookToStockOnlyOnceWhenCalledConcurrently() throws Exception {
        Long loanId = loanService.insert(new LoanRequestDTO(userId, BOOK_ID)).id();

        List<Throwable> failures = runConcurrently(() -> loanService.cancel(loanId));

        assertThat(failures).hasSize(THREADS - 1).allMatch(e -> e instanceof StatusInvalidException);
        assertThat(availableCopies()).isEqualTo(1);
    }

    /**
     * Dispara a mesma operação em várias threads ao mesmo tempo (todas esperam no latch
     * e são liberadas juntas) e devolve as exceções lançadas.
     */
    private List<Throwable> runConcurrently(Callable<?> task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return task.call();
            }));
        }

        ready.await();
        start.countDown();

        List<Throwable> failures = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                failures.add(e.getCause());
            } catch (Exception e) {
                failures.add(e);
            }
        }
        executor.shutdown();
        return failures;
    }

    private int countLoans() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_loan WHERE user_id = ?", Integer.class, userId);
    }

    private int availableCopies() {
        return jdbc.queryForObject("SELECT available_copies FROM tb_book WHERE id = ?", Integer.class, BOOK_ID);
    }
}
