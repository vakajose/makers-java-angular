package com.makers.loans.infrastructure.adapters.in.web;

import com.makers.loans.application.ports.in.AuthResult;
import com.makers.loans.application.ports.in.AuthUseCase;
import com.makers.loans.infrastructure.adapters.in.web.dto.AuthResponse;
import com.makers.loans.infrastructure.adapters.in.web.dto.LoginRequest;
import com.makers.loans.infrastructure.adapters.in.web.mapper.WebMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final WebMapper mapper;

    public AuthController(AuthUseCase authUseCase, WebMapper mapper) {
        this.authUseCase = authUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authUseCase.login(request.email(), request.password());
        return ResponseEntity.ok(mapper.toAuthResponse(result));
    }
}
