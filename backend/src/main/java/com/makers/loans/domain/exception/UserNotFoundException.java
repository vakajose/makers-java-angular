package com.makers.loans.domain.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(Long userId) {
        super("No se encontró el usuario con id " + userId);
    }
}
