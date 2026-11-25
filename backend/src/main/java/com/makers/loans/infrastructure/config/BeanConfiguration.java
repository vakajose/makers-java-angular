package com.makers.loans.infrastructure.config;

import com.makers.loans.application.ports.in.AuthUseCase;
import com.makers.loans.application.ports.in.ManageUsersUseCase;
import com.makers.loans.application.ports.out.LoanRepositoryPort;
import com.makers.loans.application.ports.out.PasswordEncoderPort;
import com.makers.loans.application.ports.out.TokenProviderPort;
import com.makers.loans.application.ports.out.UserRepositoryPort;
import com.makers.loans.application.services.AuthService;
import com.makers.loans.application.services.LoanService;
import com.makers.loans.application.services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public AuthUseCase authUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            TokenProviderPort tokenProviderPort) {
        return new AuthService(userRepositoryPort, passwordEncoderPort, tokenProviderPort);
    }

    @Bean
    public LoanService loanService(
            LoanRepositoryPort loanRepositoryPort,
            UserRepositoryPort userRepositoryPort) {
        return new LoanService(loanRepositoryPort, userRepositoryPort);
    }

    @Bean
    public ManageUsersUseCase manageUsersUseCase(
            UserRepositoryPort userRepositoryPort,
            LoanRepositoryPort loanRepositoryPort,
            PasswordEncoderPort passwordEncoderPort) {
        return new UserService(userRepositoryPort, loanRepositoryPort, passwordEncoderPort);
    }
}
