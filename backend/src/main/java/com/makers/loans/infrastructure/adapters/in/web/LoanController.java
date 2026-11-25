package com.makers.loans.infrastructure.adapters.in.web;

import com.makers.loans.application.ports.in.GetLoansUseCase;
import com.makers.loans.application.ports.in.ManageLoanStatusUseCase;
import com.makers.loans.application.ports.in.RequestLoanUseCase;
import com.makers.loans.domain.model.Loan;
import com.makers.loans.infrastructure.adapters.in.web.dto.CreateLoanRequest;
import com.makers.loans.infrastructure.adapters.in.web.dto.LoanResponse;
import com.makers.loans.infrastructure.adapters.in.web.mapper.WebMapper;
import com.makers.loans.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final RequestLoanUseCase requestLoanUseCase;
    private final GetLoansUseCase getLoansUseCase;
    private final ManageLoanStatusUseCase manageLoanStatusUseCase;
    private final WebMapper mapper;

    public LoanController(
            RequestLoanUseCase requestLoanUseCase,
            GetLoansUseCase getLoansUseCase,
            ManageLoanStatusUseCase manageLoanStatusUseCase,
            WebMapper mapper) {
        this.requestLoanUseCase = requestLoanUseCase;
        this.getLoansUseCase = getLoansUseCase;
        this.manageLoanStatusUseCase = manageLoanStatusUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<LoanResponse> requestLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Loan created = requestLoanUseCase.requestLoan(
                currentUser.getId(),
                request.amount(),
                request.termMonths()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toLoanResponse(created));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<LoanResponse>> getMyLoans(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<LoanResponse> responses = getLoansUseCase.getLoansByUser(currentUser.getId())
                .stream()
                .map(mapper::toLoanResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanResponse>> getAllLoans() {
        List<LoanResponse> responses = getLoansUseCase.getAllLoans()
                .stream()
                .map(mapper::toLoanResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<LoanResponse> getLoanById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Loan loan = getLoansUseCase.getLoanById(id, currentUser.getId(), currentUser.isAdmin());
        return ResponseEntity.ok(mapper.toLoanResponse(loan));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanResponse> approveLoan(@PathVariable Long id) {
        Loan approved = manageLoanStatusUseCase.approveLoan(id);
        return ResponseEntity.ok(mapper.toLoanResponse(approved));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanResponse> rejectLoan(@PathVariable Long id) {
        Loan rejected = manageLoanStatusUseCase.rejectLoan(id);
        return ResponseEntity.ok(mapper.toLoanResponse(rejected));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<LoanResponse> cancelLoan(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Loan cancelled = manageLoanStatusUseCase.cancelLoan(id, currentUser.getId(), currentUser.isAdmin());
        return ResponseEntity.ok(mapper.toLoanResponse(cancelled));
    }
}
