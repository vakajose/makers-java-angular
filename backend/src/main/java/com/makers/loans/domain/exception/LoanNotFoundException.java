package com.makers.loans.domain.exception;

public class LoanNotFoundException extends DomainException {
    public LoanNotFoundException(Long loanId) {
        super("No se encontró el préstamo con id " + loanId);
    }
}
