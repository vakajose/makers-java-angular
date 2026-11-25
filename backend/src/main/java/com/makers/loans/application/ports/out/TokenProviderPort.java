package com.makers.loans.application.ports.out;

import com.makers.loans.domain.model.User;

public interface TokenProviderPort {
    String generateToken(User user);
}
