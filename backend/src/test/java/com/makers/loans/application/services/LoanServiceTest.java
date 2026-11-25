package com.makers.loans.application.services;

import com.makers.loans.application.ports.out.LoanRepositoryPort;
import com.makers.loans.application.ports.out.UserRepositoryPort;
import com.makers.loans.domain.exception.InvalidLoanStateException;
import com.makers.loans.domain.exception.LoanAccessDeniedException;
import com.makers.loans.domain.model.Loan;
import com.makers.loans.domain.model.LoanStatus;
import com.makers.loans.domain.model.Role;
import com.makers.loans.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepositoryPort loanRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private LoanService loanService;

    @Test
    void requestLoanCreatesPendingLoanForAuthenticatedUser() {
        User user = User.reconstitute(1L, "Usuario", "usuario@test.com", "hash", Role.USER, Instant.now());
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(user));
        when(loanRepositoryPort.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan created = loanService.requestLoan(1L, new BigDecimal("1000"), 12);

        assertThat(created.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(created.getUserId()).isEqualTo(1L);
    }

    @Test
    void getLoanByIdRejectsDifferentNonAdminUser() {
        Loan loan = Loan.reconstitute(10L, 1L, "u1@test.com", new BigDecimal("1000"), 12,
                LoanStatus.PENDING, Instant.now(), Instant.now(), 0L);
        when(loanRepositoryPort.findById(10L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.getLoanById(10L, 2L, false))
                .isInstanceOf(LoanAccessDeniedException.class);
    }

    @Test
    void approveAndRejectTransitionPendingLoans() {
        Loan pending = Loan.reconstitute(10L, 1L, "u1@test.com", new BigDecimal("1000"), 12,
                LoanStatus.PENDING, Instant.now(), Instant.now(), 0L);
        when(loanRepositoryPort.findById(10L)).thenReturn(Optional.of(pending));
        when(loanRepositoryPort.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan approved = loanService.approveLoan(10L);
        assertThat(approved.getStatus()).isEqualTo(LoanStatus.APPROVED);

        when(loanRepositoryPort.findById(10L)).thenReturn(Optional.of(approved));
        assertThatThrownBy(() -> loanService.rejectLoan(10L))
                .isInstanceOf(InvalidLoanStateException.class);
    }

    @Test
    void cancelLoanAllowsOwnerAndAdminOnPendingAndRejectsNonOwner() {
        Loan pending = Loan.reconstitute(15L, 1L, "u1@test.com", new BigDecimal("2500"), 12,
                LoanStatus.PENDING, Instant.now(), Instant.now(), 0L);
        when(loanRepositoryPort.findById(15L)).thenReturn(Optional.of(pending));
        when(loanRepositoryPort.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> loanService.cancelLoan(15L, 2L, false))
                .isInstanceOf(LoanAccessDeniedException.class);

        Loan cancelled = loanService.cancelLoan(15L, 1L, false);
        assertThat(cancelled.getStatus()).isEqualTo(LoanStatus.CANCELLED);

        when(loanRepositoryPort.findById(15L)).thenReturn(Optional.of(cancelled));
        assertThatThrownBy(() -> loanService.cancelLoan(15L, 99L, true))
                .isInstanceOf(InvalidLoanStateException.class);
    }
}
