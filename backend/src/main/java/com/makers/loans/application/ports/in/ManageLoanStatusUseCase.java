package com.makers.loans.application.ports.in;

import com.makers.loans.domain.model.Loan;

public interface ManageLoanStatusUseCase {
    Loan approveLoan(Long loanId);

    Loan rejectLoan(Long loanId);

    Loan cancelLoan(Long loanId, Long requesterId, boolean isAdmin);
}
