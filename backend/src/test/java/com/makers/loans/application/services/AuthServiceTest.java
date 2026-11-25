package com.makers.loans.application.services;

import com.makers.loans.application.ports.in.AuthResult;
import com.makers.loans.application.ports.out.PasswordEncoderPort;
import com.makers.loans.application.ports.out.TokenProviderPort;
import com.makers.loans.application.ports.out.UserRepositoryPort;
import com.makers.loans.domain.exception.InvalidCredentialsException;
import com.makers.loans.domain.model.Role;
import com.makers.loans.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private TokenProviderPort tokenProviderPort;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginSuccessReturnsTokenAndUser() {
        User user = User.reconstitute(1L, "Usuario", "usuario@test.com", "hashed", Role.USER, Instant.now());
        when(userRepositoryPort.findByEmail("usuario@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("123", "hashed")).thenReturn(true);
        when(tokenProviderPort.generateToken(user)).thenReturn("jwt-token");

        AuthResult result = authService.login(" USUARIO@TEST.COM ", "123");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user().getEmail()).isEqualTo("usuario@test.com");
    }

    @Test
    void loginFailsWithInvalidPassword() {
        User user = User.reconstitute(1L, "Usuario", "usuario@test.com", "hashed", Role.USER, Instant.now());
        when(userRepositoryPort.findByEmail("usuario@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("usuario@test.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
