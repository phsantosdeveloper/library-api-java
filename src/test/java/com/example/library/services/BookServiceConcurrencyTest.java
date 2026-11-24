package com.example.library.services;

import com.example.library.dtos.BookCopiesDTO;
import com.example.library.dtos.BookUpdateDTO;
import com.example.library.dtos.LoanRequestDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
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
class BookServiceConcurrencyTest {

    private static final int THREADS = 10;
    private static final long BOOK_ID = 3L;

    @Autowired
    private BookService bookService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer originalCopies;

    @BeforeEach
    void setUp() {
        originalCopies = availableCopies();
        jdbc.update("UPDATE tb_book SET available_copies = ? WHERE id = ?", THREADS, BOOK_ID);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("UPDATE tb_book SET available_copies = ? WHERE id = ?", originalCopies, BOOK_ID);
    }

    @Test
    void addBookCopiesShouldNotLoseUpdatesWhenCalledConcurrently() throws Exception {
        List<Throwable> failures = runConcurrently(() -> bookService.addBookCopies(BOOK_ID, new BookCopiesDTO(1)));

        assertThat(failures).isEmpty();
        assertThat(availableCopies()).isEqualTo(THREADS * 2);
    }

    @Test
    void removeBookCopiesShouldNotLoseUpdatesWhenCalledConcurrently() throws Exception {
        List<Throwable> failures = runConcurrently(() -> bookService.removeBookCopies(BOOK_ID, new BookCopiesDTO(1)));

        assertThat(failures).isEmpty();
        assertThat(availableCopies()).isZero();
    }

    @Test
    void updateShouldNotOverwriteStockChangedByConcurrentLoan() throws Exception {
        long bookId = 7L;
        String email = "book.update.concurrency@example.com";
        jdbc.update("INSERT INTO tb_user (name, email, phone, password) VALUES ('Concurrency Test', ?, '(11) 90000-9998', 'x')", email);
        Long userId = jdbc.queryForObject("SELECT id FROM tb_user WHERE email = ?", Long.class, email);
        String originalTitle = jdbc.queryForObject("SELECT title FROM tb_book WHERE id = ?", String.class, bookId);
        int copiesBefore = jdbc.queryForObject("SELECT available_copies FROM tb_book WHERE id = ?", Integer.class, bookId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Abre a transação do update e, antes do commit, dispara um empréstimo do mesmo livro em outra
            // transação. Com o lock, o empréstimo espera o commit do update e decrementa o valor já gravado.
            Future<?> loan = transactionTemplate.execute(status -> {
                bookService.update(bookId, new BookUpdateDTO("Título atualizado", "9788535909555",
                        LocalDate.of(2007, 1, 10), 3L, 3L));
                Future<?> f = executor.submit(() -> loanService.insert(new LoanRequestDTO(userId, bookId)));
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return f;
            });
            loan.get(30, TimeUnit.SECONDS);

            assertThat(jdbc.queryForObject("SELECT available_copies FROM tb_book WHERE id = ?", Integer.class, bookId))
                    .isEqualTo(copiesBefore - 1);
        } finally {
            executor.shutdown();
            jdbc.update("DELETE FROM tb_loan WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM tb_user WHERE id = ?", userId);
            jdbc.update("UPDATE tb_book SET title = ?, available_copies = ? WHERE id = ?", originalTitle, copiesBefore, bookId);
        }
    }

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

    private int availableCopies() {
        return jdbc.queryForObject("SELECT available_copies FROM tb_book WHERE id = ?", Integer.class, BOOK_ID);
    }
}
