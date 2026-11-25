package com.makers.loans.application.services;

import com.makers.loans.application.ports.out.LoanRepositoryPort;
import com.makers.loans.application.ports.out.PasswordEncoderPort;
import com.makers.loans.application.ports.out.UserRepositoryPort;
import com.makers.loans.domain.exception.DuplicateEmailException;
import com.makers.loans.domain.exception.InvalidUserOperationException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private LoanRepositoryPort loanRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserEncodesPasswordAndSaves() {
        when(userRepositoryPort.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoderPort.encode("secret")).thenReturn("encoded");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.createUser("Nuevo Admin", "nuevo@test.com", "secret", Role.ADMIN);

        assertThat(created.getRole()).isEqualTo(Role.ADMIN);
        assertThat(created.getPasswordHash()).isEqualTo("encoded");
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepositoryPort.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser("Dup", "dup@test.com", "123", Role.USER))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateUserBlocksSelfDemotion() {
        User admin = User.reconstitute(1L, "Admin", "admin@test.com", "hash", Role.ADMIN, Instant.now());
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.updateUser(1L, 1L, "Admin", "admin@test.com", Role.USER, null))
                .isInstanceOf(InvalidUserOperationException.class);
    }

    @Test
    void deleteUserBlocksSelfDeletionAndUsersWithLoans() {
        assertThatThrownBy(() -> userService.deleteUser(1L, 1L))
                .isInstanceOf(InvalidUserOperationException.class);

        User client = User.reconstitute(2L, "Cliente", "cli@test.com", "hash", Role.USER, Instant.now());
        when(userRepositoryPort.findById(2L)).thenReturn(Optional.of(client));
        when(loanRepositoryPort.existsByUserId(2L)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(2L, 1L))
                .isInstanceOf(InvalidUserOperationException.class);

        when(loanRepositoryPort.existsByUserId(2L)).thenReturn(false);
        userService.deleteUser(2L, 1L);
        verify(userRepositoryPort).deleteById(2L);
    }
}
