package com.makers.loans.application.ports.in;

import com.makers.loans.domain.model.Loan;

import java.util.List;

public interface GetLoansUseCase {
    List<Loan> getLoansByUser(Long userId);

    List<Loan> getAllLoans();

    Loan getLoanById(Long loanId, Long requesterId, boolean isAdmin);
}
