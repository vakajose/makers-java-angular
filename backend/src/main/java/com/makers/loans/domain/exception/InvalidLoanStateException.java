package com.makers.loans.domain.exception;

public class InvalidLoanStateException extends DomainException {
    public InvalidLoanStateException(String message) {
        super(message);
    }
}
