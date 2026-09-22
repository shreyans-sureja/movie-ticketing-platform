package com.dmg.movieticketing.shared.api;

import com.dmg.movieticketing.identity.application.exception.AccountNotFoundException;
import com.dmg.movieticketing.identity.application.exception.EmailAlreadyRegisteredException;
import com.dmg.movieticketing.identity.application.exception.InvalidCredentialsException;
import com.dmg.movieticketing.identity.application.exception.PasswordPolicyViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiProblem> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(
                        error.getField(),
                        validationCode(error.getField(), error.getCode()),
                        error.getDefaultMessage()
                ))
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::code))
                .toList();

        ApiProblem problem = new ApiProblem(
                "urn:movie-ticketing:problem:validation-failed",
                "Invalid request",
                HttpStatus.BAD_REQUEST.value(),
                "One or more fields are invalid.",
                request.getRequestURI(),
                "VALIDATION_FAILED",
                violations
        );
        return problem(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiProblem> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                ApiProblem.of(
                        "urn:movie-ticketing:problem:malformed-request",
                        "Malformed request",
                        HttpStatus.BAD_REQUEST.value(),
                        "The request body is missing or malformed.",
                        request.getRequestURI(),
                        "MALFORMED_REQUEST"
                )
        );
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    ResponseEntity<ApiProblem> handlePasswordPolicy(
            PasswordPolicyViolationException exception,
            HttpServletRequest request
    ) {
        ApiProblem problem = new ApiProblem(
                "urn:movie-ticketing:problem:password-policy",
                "Invalid request",
                HttpStatus.BAD_REQUEST.value(),
                "The password does not satisfy the password policy.",
                request.getRequestURI(),
                "VALIDATION_FAILED",
                List.of(new FieldViolation("password", "PASSWORD_TOO_LONG_FOR_BCRYPT", exception.getMessage()))
        );
        return problem(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiProblem> handleDuplicateEmail(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                ApiProblem.of(
                        "urn:movie-ticketing:problem:email-already-registered",
                        "Email already registered",
                        HttpStatus.CONFLICT.value(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        "EMAIL_ALREADY_REGISTERED"
                )
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiProblem> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        ApiProblem problem = ApiProblem.of(
                "urn:movie-ticketing:problem:invalid-credentials",
                "Authentication failed",
                HttpStatus.UNAUTHORIZED.value(),
                exception.getMessage(),
                request.getRequestURI(),
                "INVALID_CREDENTIALS"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(PROBLEM_JSON)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problem);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ApiProblem> handleAccountNotFound(
            AccountNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                ApiProblem.of(
                        "urn:movie-ticketing:problem:account-not-found",
                        "Account not found",
                        HttpStatus.NOT_FOUND.value(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        "ACCOUNT_NOT_FOUND"
                )
        );
    }

    private String validationCode(String field, String beanValidationCode) {
        String normalizedField = field.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        String normalizedConstraint = beanValidationCode
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
        return normalizedField + "_" + normalizedConstraint;
    }

    private ResponseEntity<ApiProblem> problem(HttpStatus status, ApiProblem problem) {
        return ResponseEntity.status(status).contentType(PROBLEM_JSON).body(problem);
    }
}
