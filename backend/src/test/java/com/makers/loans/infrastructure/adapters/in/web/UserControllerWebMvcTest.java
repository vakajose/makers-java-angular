package com.makers.loans.infrastructure.adapters.in.web;

import com.makers.loans.application.ports.in.ManageUsersUseCase;
import com.makers.loans.domain.exception.DuplicateEmailException;
import com.makers.loans.domain.model.Role;
import com.makers.loans.domain.model.User;
import com.makers.loans.infrastructure.adapters.in.web.mapper.WebMapper;
import com.makers.loans.infrastructure.security.AuthenticatedUser;
import com.makers.loans.infrastructure.security.JwtAuthenticationFilter;
import com.makers.loans.infrastructure.security.JwtService;
import com.makers.loans.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, WebMapper.class})
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ManageUsersUseCase manageUsersUseCase;

    private AuthenticatedUser clientPrincipal() {
        return new AuthenticatedUser(1L, "usuario@test.com", "Usuario Demo", Role.USER);
    }

    private AuthenticatedUser adminPrincipal() {
        return new AuthenticatedUser(2L, "admin@test.com", "Admin Admin", Role.ADMIN);
    }

    @Test
    void authenticatedUserCanFetchOwnProfile() throws Exception {
        User current = User.reconstitute(1L, "Usuario Demo", "usuario@test.com", "h", Role.USER, Instant.now());
        when(manageUsersUseCase.getCurrentUser(1L)).thenReturn(current);

        mockMvc.perform(get("/api/users/me").with(user(clientPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("usuario@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void nonAdminCannotListOrCreateUsersReturns403() throws Exception {
        mockMvc.perform(get("/api/users").with(user(clientPrincipal())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .with(user(clientPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@test.com\",\"password\":\"123\",\"role\":\"USER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUserAndHandlesValidationAndDuplicateConflict() throws Exception {
        User created = User.reconstitute(3L, "Nuevo Admin", "nuevo@test.com", "h", Role.ADMIN, Instant.now());
        when(manageUsersUseCase.createUser("Nuevo Admin", "nuevo@test.com", "123", Role.ADMIN))
                .thenReturn(created);

        mockMvc.perform(post("/api/users")
                        .with(user(adminPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nuevo Admin\",\"email\":\"nuevo@test.com\",\"password\":\"123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/users")
                        .with(user(adminPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"1\",\"role\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());

        when(manageUsersUseCase.createUser("Dup", "usuario@test.com", "123", Role.USER))
                .thenThrow(new DuplicateEmailException("usuario@test.com"));

        mockMvc.perform(post("/api/users")
                        .with(user(adminPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dup\",\"email\":\"usuario@test.com\",\"password\":\"123\",\"role\":\"USER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
