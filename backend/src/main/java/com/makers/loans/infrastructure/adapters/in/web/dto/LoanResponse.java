package com.makers.loans.infrastructure.adapters.in.web.dto;

import com.makers.loans.domain.model.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanResponse(
        Long id,
        Long userId,
        String userEmail,
        BigDecimal amount,
        int termMonths,
        LoanStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
