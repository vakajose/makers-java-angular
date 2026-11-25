package com.makers.loans.application.ports.in;

import com.makers.loans.domain.model.Role;
import com.makers.loans.domain.model.User;

import java.util.List;

public interface ManageUsersUseCase {
    User getCurrentUser(Long userId);

    List<User> getAllUsers();

    User createUser(String name, String email, String rawPassword, Role role);

    User updateUser(
            Long targetUserId,
            Long actorUserId,
            String name,
            String email,
            Role role,
            String rawPasswordOrNull);

    void deleteUser(Long targetUserId, Long actorUserId);
}
