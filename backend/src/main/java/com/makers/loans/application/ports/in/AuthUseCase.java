package com.makers.loans.application.ports.in;

public interface AuthUseCase {
    AuthResult login(String email, String rawPassword);
}
