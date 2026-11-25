package com.makers.loans.infrastructure.config;

import com.makers.loans.application.ports.out.PasswordEncoderPort;
import com.makers.loans.application.ports.out.UserRepositoryPort;
import com.makers.loans.domain.model.Role;
import com.makers.loans.domain.model.User;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public DataInitializer(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUserIfAbsent("Usuario Demo", "usuario@test.com", "123", Role.USER);
        seedUserIfAbsent("Admin Admin", "admin@test.com", "123", Role.ADMIN);
    }

    private void seedUserIfAbsent(String name, String email, String rawPassword, Role role) {
        if (!userRepositoryPort.existsByEmail(email)) {
            User user = User.create(name, email, passwordEncoderPort.encode(rawPassword), role);
            userRepositoryPort.save(user);
        }
    }
}
