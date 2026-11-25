package com.makers.loans.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateLoanRequest(
        @NotNull(message = "El monto es obligatorio.")
        @DecimalMin(value = "1.00", message = "El monto mínimo permitido es 1.00.")
        @DecimalMax(value = "1000000000.00", message = "El monto máximo permitido es 1,000,000,000.00.")
        BigDecimal amount,

        @NotNull(message = "El plazo en meses es obligatorio.")
        @Min(value = 1, message = "El plazo mínimo es de 1 mes.")
        @Max(value = 360, message = "El plazo máximo es de 360 meses.")
        Integer termMonths) {
}
