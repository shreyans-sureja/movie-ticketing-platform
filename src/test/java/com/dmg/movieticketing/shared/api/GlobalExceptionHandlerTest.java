package com.dmg.movieticketing.shared.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void malformedBodyUsesStandardProblem() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/movies");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Malformed JSON",
                new MockHttpInputMessage(new byte[0])
        );

        var response = handler.handleUnreadableRequest(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MALFORMED_REQUEST");
        assertThat(response.getBody().instance()).isEqualTo("/api/v1/movies");
        assertThat(response.getBody().violations()).isEmpty();
    }

    @Test
    void missingQueryParameterUsesStandardValidationProblem() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/shows");
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("cityId", "long");

        var response = handler.handleMissingRequestParameter(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().instance()).isEqualTo("/api/v1/shows");
        assertThat(response.getBody().violations()).containsExactly(
                new FieldViolation("cityId", "CITY_ID_REQUIRED", "Required request parameter is missing.")
        );
    }

    @Test
    void invalidQueryParameterUsesStandardValidationProblem() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/shows");
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "not-a-date",
                LocalDate.class,
                "date",
                null,
                new IllegalArgumentException("invalid date")
        );

        var response = handler.handleRequestParameterTypeMismatch(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().violations()).containsExactly(
                new FieldViolation("date", "DATE_TYPE", "Request parameter has an invalid value.")
        );
    }
}
