package com.dmg.movieticketing.identity.security;

import com.dmg.movieticketing.shared.api.ApiProblem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Writes the same problem-details format for failures raised inside Spring Security filters. */
@Component
public class SecurityProblemWriter {

    private final ObjectMapper objectMapper;

    public SecurityProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        boolean expired = isExpired(exception);
        String code = expired ? "TOKEN_EXPIRED" : "INVALID_TOKEN";
        String detail = expired ? "The bearer token has expired." : "A valid bearer token is required.";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("WWW-Authenticate", "Bearer");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiProblem.of(
                        "urn:movie-ticketing:problem:" + code.toLowerCase().replace('_', '-'),
                        "Authentication required",
                        HttpServletResponse.SC_UNAUTHORIZED,
                        detail,
                        request.getRequestURI(),
                        code
                )
        );
    }

    public void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiProblem.of(
                        "urn:movie-ticketing:problem:forbidden",
                        "Forbidden",
                        HttpServletResponse.SC_FORBIDDEN,
                        "The authenticated account is not allowed to perform this operation.",
                        request.getRequestURI(),
                        "FORBIDDEN"
                )
        );
    }

    private boolean isExpired(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof JwtValidationException validationException) {
                return validationException.getErrors().stream()
                        .anyMatch(error -> error.getDescription().toLowerCase().contains("expired"));
            }
            current = current.getCause();
        }
        return false;
    }
}
