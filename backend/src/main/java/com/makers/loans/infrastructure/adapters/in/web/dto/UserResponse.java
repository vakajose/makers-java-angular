package com.makers.loans.infrastructure.adapters.in.web.dto;

import com.makers.loans.domain.model.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt) {
}
