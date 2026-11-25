package com.makers.loans.infrastructure.adapters.in.web;

import com.makers.loans.application.ports.in.GetLoansUseCase;
import com.makers.loans.application.ports.in.ManageLoanStatusUseCase;
import com.makers.loans.application.ports.in.RequestLoanUseCase;
import com.makers.loans.domain.exception.InvalidLoanStateException;
import com.makers.loans.domain.model.Loan;
import com.makers.loans.domain.model.LoanStatus;
import com.makers.loans.domain.model.Role;
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

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoanController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, WebMapper.class})
class LoanControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RequestLoanUseCase requestLoanUseCase;

    @MockitoBean
    private GetLoansUseCase getLoansUseCase;

    @MockitoBean
    private ManageLoanStatusUseCase manageLoanStatusUseCase;

    private AuthenticatedUser clientPrincipal() {
        return new AuthenticatedUser(1L, "usuario@test.com", "Usuario Demo", Role.USER);
    }

    private AuthenticatedUser adminPrincipal() {
        return new AuthenticatedUser(2L, "admin@test.com", "Admin Admin", Role.ADMIN);
    }

    @Test
    void unauthenticatedRequestReturns401ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000,\"termMonths\":12}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidPayloadReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .with(user(clientPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"termMonths\":500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.termMonths").exists());
    }

    @Test
    void userRoleCannotApproveLoanReturns403() throws Exception {
        mockMvc.perform(patch("/api/loans/1/approve").with(user(clientPrincipal())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void approveAlreadyProcessedLoanReturns409ProblemDetail() throws Exception {
        when(manageLoanStatusUseCase.approveLoan(99L))
                .thenThrow(new InvalidLoanStateException("No se puede aprobar el préstamo en estado APPROVED."));

        mockMvc.perform(patch("/api/loans/99/approve").with(user(adminPrincipal())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflicto de negocio"));
    }

    @Test
    void ownerCanCancelPendingLoanReturns200Cancelled() throws Exception {
        Loan cancelled = Loan.reconstitute(5L, 1L, "usuario@test.com", new BigDecimal("1000.00"),
                12, LoanStatus.CANCELLED, Instant.now(), Instant.now(), 1L);
        when(manageLoanStatusUseCase.cancelLoan(5L, 1L, false)).thenReturn(cancelled);

        mockMvc.perform(delete("/api/loans/5").with(user(clientPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
