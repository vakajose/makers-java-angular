package com.makers.loans.domain.model;

import com.makers.loans.domain.exception.InvalidUserDataException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class User {

    private final Long id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private final Instant createdAt;

    private User(
            Long id,
            String name,
            String email,
            String passwordHash,
            Role role,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static User create(String name, String email, String passwordHash, Role role) {
        String validName = validateName(name);
        String validEmail = validateAndNormalizeEmail(email);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidUserDataException("La contraseña cifrada del usuario no puede estar vacía.");
        }
        if (role == null) {
            throw new InvalidUserDataException("El rol del usuario es obligatorio.");
        }
        return new User(null, validName, validEmail, passwordHash, role, Instant.now());
    }

    public static User reconstitute(
            Long id,
            String name,
            String email,
            String passwordHash,
            Role role,
            Instant createdAt) {
        return new User(
                id,
                name,
                email,
                passwordHash,
                role,
                createdAt != null ? createdAt : Instant.now()
        );
    }

    public void updateProfile(String name, String email, Role role, String newPasswordHashOrNull) {
        this.name = validateName(name);
        this.email = validateAndNormalizeEmail(email);
        if (role == null) {
            throw new InvalidUserDataException("El rol del usuario es obligatorio.");
        }
        this.role = role;
        if (newPasswordHashOrNull != null && !newPasswordHashOrNull.isBlank()) {
            this.passwordHash = newPasswordHashOrNull;
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidUserDataException("El nombre del usuario es obligatorio.");
        }
        return name.trim();
    }

    private static String validateAndNormalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidUserDataException("El correo electrónico es obligatorio.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0 || atIndex == normalized.length() - 1 || normalized.contains(" ")) {
            throw new InvalidUserDataException("El correo electrónico no tiene un formato válido.");
        }
        return normalized;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id) && Objects.equals(email, other.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}
