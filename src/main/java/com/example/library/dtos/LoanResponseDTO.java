package com.example.library.dtos;

import com.example.library.entities.Loan;
import com.example.library.entities.enums.LoanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanResponseDTO(

        Long id,
        LocalDate loanDate,
        LocalDate expectedReturnDate,
        LocalDateTime timeReturn,
        LoanStatus status,
        UserMinDTO user,
        BookMinDTO book

) {

    public LoanResponseDTO(Loan loan) {
        this(loan.getId(), loan.getLoanDate(), loan.getExpectedReturnDate(),
                loan.getTimeReturn(), loan.getStatus(), new UserMinDTO(loan.getUser()),
                new BookMinDTO(loan.getBook()));
    }
}
