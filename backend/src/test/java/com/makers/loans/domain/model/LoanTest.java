package com.makers.loans.domain.model;

import com.makers.loans.domain.exception.InvalidLoanDataException;
import com.makers.loans.domain.exception.InvalidLoanStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    @Test
    void requestCreatesPendingLoanWithNormalizedScale() {
        Loan loan = Loan.request(1L, "usuario@test.com", new BigDecimal("1500.5"), 12);

        assertThat(loan.getUserId()).isEqualTo(1L);
        assertThat(loan.getUserEmail()).isEqualTo("usuario@test.com");
        assertThat(loan.getAmount()).isEqualByComparingTo("1500.50");
        assertThat(loan.getTermMonths()).isEqualTo(12);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(loan.isOwnedBy(1L)).isTrue();
        assertThat(loan.isOwnedBy(2L)).isFalse();
    }

    @Test
    void requestRejectsInvalidAmountOrTerm() {
        assertThatThrownBy(() -> Loan.request(1L, "u@test.com", BigDecimal.ZERO, 12))
                .isInstanceOf(InvalidLoanDataException.class);
        assertThatThrownBy(() -> Loan.request(1L, "u@test.com", new BigDecimal("1000000001"), 12))
                .isInstanceOf(InvalidLoanDataException.class);
        assertThatThrownBy(() -> Loan.request(1L, "u@test.com", new BigDecimal("1000"), 0))
                .isInstanceOf(InvalidLoanDataException.class);
        assertThatThrownBy(() -> Loan.request(1L, "u@test.com", new BigDecimal("1000"), 361))
                .isInstanceOf(InvalidLoanDataException.class);
    }

    @Test
    void stateMachineAllowsOnlyTransitionsFromPending() {
        Loan toApprove = Loan.request(1L, "u@test.com", new BigDecimal("1000"), 12);
        toApprove.approve();
        assertThat(toApprove.getStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThatThrownBy(toApprove::reject).isInstanceOf(InvalidLoanStateException.class);
        assertThatThrownBy(toApprove::cancel).isInstanceOf(InvalidLoanStateException.class);

        Loan toReject = Loan.request(1L, "u@test.com", new BigDecimal("2000"), 24);
        toReject.reject();
        assertThat(toReject.getStatus()).isEqualTo(LoanStatus.REJECTED);
        assertThatThrownBy(toReject::approve).isInstanceOf(InvalidLoanStateException.class);

        Loan toCancel = Loan.request(1L, "u@test.com", new BigDecimal("3000"), 6);
        toCancel.cancel();
        assertThat(toCancel.getStatus()).isEqualTo(LoanStatus.CANCELLED);
        assertThatThrownBy(toCancel::approve).isInstanceOf(InvalidLoanStateException.class);
        assertThatThrownBy(toCancel::cancel).isInstanceOf(InvalidLoanStateException.class);
    }
}
