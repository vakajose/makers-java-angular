package com.makers.loans.infrastructure.adapters.in.web.dto;

import com.makers.loans.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        String name,

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El formato del correo electrónico no es válido.")
        @Size(max = 180, message = "El correo electrónico no puede superar los 180 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 3, max = 100, message = "La contraseña debe tener entre 3 y 100 caracteres.")
        String password,

        @NotNull(message = "El rol es obligatorio.")
        Role role) {
}
