package com.makers.loans.domain.exception;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Credenciales inválidas. Verifica tu correo y contraseña.");
    }
}
