package com.makers.loans.domain.exception;

public class InvalidUserOperationException extends DomainException {
    public InvalidUserOperationException(String message) {
        super(message);
    }
}
