package com.makers.loans.infrastructure.adapters.in.web.mapper;

import com.makers.loans.application.ports.in.AuthResult;
import com.makers.loans.domain.model.Loan;
import com.makers.loans.domain.model.User;
import com.makers.loans.infrastructure.adapters.in.web.dto.AuthResponse;
import com.makers.loans.infrastructure.adapters.in.web.dto.LoanResponse;
import com.makers.loans.infrastructure.adapters.in.web.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class WebMapper {

    public AuthResponse toAuthResponse(AuthResult result) {
        return new AuthResponse(result.token(), toUserResponse(result.user()));
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    public LoanResponse toLoanResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getUserId(),
                loan.getUserEmail(),
                loan.getAmount(),
                loan.getTermMonths(),
                loan.getStatus(),
                loan.getCreatedAt(),
                loan.getUpdatedAt()
        );
    }
}
