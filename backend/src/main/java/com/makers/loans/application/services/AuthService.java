package com.makers.loans.application.services;

import com.makers.loans.application.ports.in.AuthResult;
import com.makers.loans.application.ports.in.AuthUseCase;
import com.makers.loans.application.ports.out.PasswordEncoderPort;
import com.makers.loans.application.ports.out.TokenProviderPort;
import com.makers.loans.application.ports.out.UserRepositoryPort;
import com.makers.loans.domain.exception.InvalidCredentialsException;
import com.makers.loans.domain.model.User;

import java.util.Locale;

public class AuthService implements AuthUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;

    public AuthService(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            TokenProviderPort tokenProviderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    @Override
    public AuthResult login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            throw new InvalidCredentialsException();
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoderPort.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenProviderPort.generateToken(user);
        return new AuthResult(token, user);
    }
}
