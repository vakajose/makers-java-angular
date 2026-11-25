package com.makers.loans.application.ports.in;

import com.makers.loans.domain.model.User;

public record AuthResult(String token, User user) {
}
