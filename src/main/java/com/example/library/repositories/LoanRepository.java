package com.example.library.repositories;

import com.example.library.entities.Loan;
import com.example.library.entities.enums.LoanStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    boolean existsByExpectedReturnDateBeforeAndUserIdAndStatus(LocalDate now, Long userId, LoanStatus status);

    @Override
    @EntityGraph(attributePaths = {"user", "book"})
    Page<Loan> findAll(Specification<Loan> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "book"})
    Page<Loan> findByUserId(Pageable pageable, Long userId);

    // Pede ao banco no máximo 5s de espera pelo lock; estourado, lança PessimisticLockingFailureException (409).
    // Obs.: o H2 ignora essa dica e usa o próprio LOCK_TIMEOUT da sessão (padrão ~2s)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT l FROM Loan l WHERE l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);

}
