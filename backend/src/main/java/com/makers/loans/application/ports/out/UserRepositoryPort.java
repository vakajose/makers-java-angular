package com.makers.loans.application.ports.out;

import com.makers.loans.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAll();

    User save(User user);

    void deleteById(Long id);
}
