package com.makers.loans.domain.exception;

public class DuplicateEmailException extends DomainException {
    public DuplicateEmailException(String email) {
        super("Ya existe un usuario registrado con el correo: " + email);
    }
}
