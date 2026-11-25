package com.makers.loans.domain.exception;

public class LoanAccessDeniedException extends DomainException {
    public LoanAccessDeniedException(String message) {
        super(message);
    }
}
