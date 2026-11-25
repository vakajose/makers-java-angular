package com.makers.loans.application.ports.in;

import com.makers.loans.domain.model.Loan;

import java.math.BigDecimal;

public interface RequestLoanUseCase {
    Loan requestLoan(Long userId, BigDecimal amount, int termMonths);
}
