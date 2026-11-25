package com.makers.loans.application.services;

import com.makers.loans.application.ports.in.ManageUsersUseCase;
import com.makers.loans.application.ports.out.LoanRepositoryPort;
import com.makers.loans.application.ports.out.PasswordEncoderPort;
import com.makers.loans.application.ports.out.UserRepositoryPort;
import com.makers.loans.domain.exception.DuplicateEmailException;
import com.makers.loans.domain.exception.InvalidUserDataException;
import com.makers.loans.domain.exception.InvalidUserOperationException;
import com.makers.loans.domain.exception.UserNotFoundException;
import com.makers.loans.domain.model.Role;
import com.makers.loans.domain.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UserService implements ManageUsersUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final LoanRepositoryPort loanRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public UserService(
            UserRepositoryPort userRepositoryPort,
            LoanRepositoryPort loanRepositoryPort,
            PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.loanRepositoryPort = loanRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(Long userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepositoryPort.findAll();
    }

    @Override
    @Transactional
    public User createUser(String name, String email, String rawPassword, Role role) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail != null && userRepositoryPort.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }
        validateRawPassword(rawPassword, true);
        String passwordHash = passwordEncoderPort.encode(rawPassword);
        User user = User.create(name, email, passwordHash, role);
        return userRepositoryPort.save(user);
    }

    @Override
    @Transactional
    public User updateUser(
            Long targetUserId,
            Long actorUserId,
            String name,
            String email,
            Role role,
            String rawPasswordOrNull) {
        User existing = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (Objects.equals(targetUserId, actorUserId) && role != null && role != Role.ADMIN) {
            throw new InvalidUserOperationException(
                    "No puedes degradar tu propio rol de administrador.");
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail != null) {
            userRepositoryPort.findByEmail(normalizedEmail)
                    .filter(found -> !Objects.equals(found.getId(), targetUserId))
                    .ifPresent(found -> {
                        throw new DuplicateEmailException(normalizedEmail);
                    });
        }

        String newHash = null;
        if (rawPasswordOrNull != null && !rawPasswordOrNull.isBlank()) {
            validateRawPassword(rawPasswordOrNull, false);
            newHash = passwordEncoderPort.encode(rawPasswordOrNull);
        }

        existing.updateProfile(name, email, role, newHash);
        return userRepositoryPort.save(existing);
    }

    @Override
    @Transactional
    public void deleteUser(Long targetUserId, Long actorUserId) {
        if (Objects.equals(targetUserId, actorUserId)) {
            throw new InvalidUserOperationException(
                    "No puedes eliminar tu propia cuenta de administrador.");
        }
        User existing = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (loanRepositoryPort.existsByUserId(existing.getId())) {
            throw new InvalidUserOperationException(
                    "No se puede eliminar un usuario con préstamos registrados.");
        }

        userRepositoryPort.deleteById(existing.getId());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateRawPassword(String rawPassword, boolean required) {
        if (rawPassword == null || rawPassword.isBlank()) {
            if (required) {
                throw new InvalidUserDataException("La contraseña es obligatoria.");
            }
            return;
        }
        if (rawPassword.length() < 3) {
            throw new InvalidUserDataException(
                    "La contraseña debe tener al menos 3 caracteres.");
        }
    }
}
