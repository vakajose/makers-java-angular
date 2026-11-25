package com.makers.loans.infrastructure.adapters.in.web;

import com.makers.loans.domain.exception.DuplicateEmailException;
import com.makers.loans.domain.exception.InvalidCredentialsException;
import com.makers.loans.domain.exception.InvalidLoanDataException;
import com.makers.loans.domain.exception.InvalidLoanStateException;
import com.makers.loans.domain.exception.InvalidUserDataException;
import com.makers.loans.domain.exception.InvalidUserOperationException;
import com.makers.loans.domain.exception.LoanAccessDeniedException;
import com.makers.loans.domain.exception.LoanNotFoundException;
import com.makers.loans.domain.exception.UserNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Datos de entrada inválidos",
                "Uno o más campos no superaron la validación.",
                request
        );
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Cuerpo de petición inválido",
                "El formato JSON recibido no es válido o contiene tipos incompatibles.",
                request
        );
    }

    @ExceptionHandler({InvalidLoanDataException.class, InvalidUserDataException.class})
    public ProblemDetail handleInvalidDomainData(RuntimeException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Datos inválidos", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Autenticación fallida", ex.getMessage(), request);
    }

    @ExceptionHandler({
            LoanAccessDeniedException.class,
            AccessDeniedException.class,
            AuthorizationDeniedException.class
    })
    public ProblemDetail handleForbidden(RuntimeException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.FORBIDDEN,
                "Acceso denegado",
                ex instanceof LoanAccessDeniedException ? ex.getMessage() : "No tienes permisos para esta operación.",
                request
        );
    }

    @ExceptionHandler({LoanNotFoundException.class, UserNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.NOT_FOUND, "Ruta no encontrada", "El recurso solicitado no existe.", request);
    }

    @ExceptionHandler({
            InvalidLoanStateException.class,
            DuplicateEmailException.class,
            InvalidUserOperationException.class
    })
    public ProblemDetail handleConflict(RuntimeException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.CONFLICT, "Conflicto de negocio", ex.getMessage(), request);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ProblemDetail handleOptimisticLock(Exception ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.CONFLICT,
                "Conflicto de concurrencia",
                "El préstamo fue modificado por otra operación simultánea. Recarga los datos e inténtalo nuevamente.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                "Ocurrió un error inesperado al procesar la solicitud.",
                request
        );
    }

    private ProblemDetail buildProblem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
