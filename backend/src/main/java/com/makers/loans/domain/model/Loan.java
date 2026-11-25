package com.makers.loans.domain.model;

import com.makers.loans.domain.exception.InvalidLoanDataException;
import com.makers.loans.domain.exception.InvalidLoanStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

public class Loan {

    public static final BigDecimal MIN_AMOUNT = BigDecimal.ONE;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");
    public static final int MIN_TERM_MONTHS = 1;
    public static final int MAX_TERM_MONTHS = 360;

    private final Long id;
    private final Long userId;
    private final String userEmail;
    private final BigDecimal amount;
    private final int termMonths;
    private LoanStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private Loan(
            Long id,
            Long userId,
            String userEmail,
            BigDecimal amount,
            int termMonths,
            LoanStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.amount = amount;
        this.termMonths = termMonths;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Loan request(Long userId, String userEmail, BigDecimal amount, int termMonths) {
        if (userId == null) {
            throw new InvalidLoanDataException("El identificador de usuario es obligatorio.");
        }
        if (amount == null
                || amount.compareTo(MIN_AMOUNT) < 0
                || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidLoanDataException(
                    "El monto solicitado debe estar entre " + MIN_AMOUNT + " y " + MAX_AMOUNT + ".");
        }
        if (termMonths < MIN_TERM_MONTHS || termMonths > MAX_TERM_MONTHS) {
            throw new InvalidLoanDataException(
                    "El plazo debe estar entre " + MIN_TERM_MONTHS + " y " + MAX_TERM_MONTHS + " meses.");
        }

        Instant now = Instant.now();
        return new Loan(
                null,
                userId,
                userEmail,
                amount.setScale(2, RoundingMode.HALF_UP),
                termMonths,
                LoanStatus.PENDING,
                now,
                now,
                null
        );
    }

    public static Loan reconstitute(
            Long id,
            Long userId,
            String userEmail,
            BigDecimal amount,
            int termMonths,
            LoanStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version) {
        return new Loan(
                id,
                userId,
                userEmail,
                amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : null,
                termMonths,
                status,
                createdAt,
                updatedAt,
                version
        );
    }

    public void approve() {
        ensurePending("aprobar");
        this.status = LoanStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public void reject() {
        ensurePending("rechazar");
        this.status = LoanStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        ensurePending("cancelar");
        this.status = LoanStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public boolean isOwnedBy(Long candidateUserId) {
        return candidateUserId != null && Objects.equals(this.userId, candidateUserId);
    }

    private void ensurePending(String action) {
        if (this.status != LoanStatus.PENDING) {
            throw new InvalidLoanStateException(
                    "No se puede " + action + " el préstamo en estado " + this.status + ".");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
