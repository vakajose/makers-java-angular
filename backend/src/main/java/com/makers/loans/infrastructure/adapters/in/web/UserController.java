package com.makers.loans.infrastructure.adapters.in.web;

import com.makers.loans.application.ports.in.ManageUsersUseCase;
import com.makers.loans.domain.model.User;
import com.makers.loans.infrastructure.adapters.in.web.dto.CreateUserRequest;
import com.makers.loans.infrastructure.adapters.in.web.dto.UpdateUserRequest;
import com.makers.loans.infrastructure.adapters.in.web.dto.UserResponse;
import com.makers.loans.infrastructure.adapters.in.web.mapper.WebMapper;
import com.makers.loans.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ManageUsersUseCase manageUsersUseCase;
    private final WebMapper mapper;

    public UserController(ManageUsersUseCase manageUsersUseCase, WebMapper mapper) {
        this.manageUsersUseCase = manageUsersUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        User user = manageUsersUseCase.getCurrentUser(currentUser.getId());
        return ResponseEntity.ok(mapper.toUserResponse(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = manageUsersUseCase.getAllUsers()
                .stream()
                .map(mapper::toUserResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = manageUsersUseCase.createUser(
                request.name(),
                request.email(),
                request.password(),
                request.role()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toUserResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        User updated = manageUsersUseCase.updateUser(
                id,
                currentUser.getId(),
                request.name(),
                request.email(),
                request.role(),
                request.password()
        );
        return ResponseEntity.ok(mapper.toUserResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        manageUsersUseCase.deleteUser(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
