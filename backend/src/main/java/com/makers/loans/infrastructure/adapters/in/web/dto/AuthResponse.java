package com.makers.loans.infrastructure.adapters.in.web.dto;

public record AuthResponse(
        String token,
        UserResponse user) {
}
