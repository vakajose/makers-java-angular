package com.makers.loans.application.services;

import com.makers.loans.application.ports.in.GetLoansUseCase;
import com.makers.loans.application.ports.in.ManageLoanStatusUseCase;
import com.makers.loans.application.ports.in.RequestLoanUseCase;
import com.makers.loans.application.ports.out.LoanRepositoryPort;
import com.makers.loans.application.ports.out.UserRepositoryPort;
import com.makers.loans.domain.exception.LoanAccessDeniedException;
import com.makers.loans.domain.exception.LoanNotFoundException;
import com.makers.loans.domain.exception.UserNotFoundException;
import com.makers.loans.domain.model.Loan;
import com.makers.loans.domain.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public class LoanService implements RequestLoanUseCase, GetLoansUseCase, ManageLoanStatusUseCase {

    private final LoanRepositoryPort loanRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public LoanService(LoanRepositoryPort loanRepositoryPort, UserRepositoryPort userRepositoryPort) {
        this.loanRepositoryPort = loanRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    @Transactional
    public Loan requestLoan(Long userId, BigDecimal amount, int termMonths) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Loan loan = Loan.request(user.getId(), user.getEmail(), amount, termMonths);
        return loanRepositoryPort.save(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Loan> getLoansByUser(Long userId) {
        return loanRepositoryPort.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Loan> getAllLoans() {
        return loanRepositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Loan getLoanById(Long loanId, Long requesterId, boolean isAdmin) {
        Loan loan = findLoanOrThrow(loanId);
        verifyAccess(loan, requesterId, isAdmin, "No tienes permisos para consultar este préstamo.");
        return loan;
    }

    @Override
    @Transactional
    public Loan approveLoan(Long loanId) {
        Loan loan = findLoanOrThrow(loanId).copy();
        loan.approve();
        return loanRepositoryPort.save(loan);
    }

    @Override
    @Transactional
    public Loan rejectLoan(Long loanId) {
        Loan loan = findLoanOrThrow(loanId).copy();
        loan.reject();
        return loanRepositoryPort.save(loan);
    }

    @Override
    @Transactional
    public Loan cancelLoan(Long loanId, Long requesterId, boolean isAdmin) {
        Loan loan = findLoanOrThrow(loanId).copy();
        verifyAccess(loan, requesterId, isAdmin, "No tienes permisos para cancelar este préstamo.");
        loan.cancel();
        return loanRepositoryPort.save(loan);
    }

    private Loan findLoanOrThrow(Long loanId) {
        return loanRepositoryPort.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
    }

    private void verifyAccess(Loan loan, Long requesterId, boolean isAdmin, String message) {
        if (!isAdmin && !loan.isOwnedBy(requesterId)) {
            throw new LoanAccessDeniedException(message);
        }
    }
}
