package com.dmg.movieticketing.shared.api;

import com.dmg.movieticketing.booking.application.BookingNotFoundException;
import com.dmg.movieticketing.booking.application.BookingNoLongerOwnsSeatsException;
import com.dmg.movieticketing.booking.application.HoldExpiredException;
import com.dmg.movieticketing.booking.application.HoldNoLongerOwnsSeatsException;
import com.dmg.movieticketing.hold.application.HoldNotFoundException;
import com.dmg.movieticketing.hold.application.SeatsUnavailableException;
import com.dmg.movieticketing.hold.application.ShowAlreadyStartedException;
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

    @Test
    void holdErrorsUseStandardProblemCodes() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/shows/00000000-0000-0000-0000-000000000001/holds"
        );

        var unavailable = handler.handleSeatsUnavailable(new SeatsUnavailableException(), request);
        var started = handler.handleShowAlreadyStarted(new ShowAlreadyStartedException(), request);
        var missing = handler.handleHoldNotFound(new HoldNotFoundException(), request);

        assertThat(unavailable.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(unavailable.getBody()).isNotNull();
        assertThat(unavailable.getBody().code()).isEqualTo("SEATS_UNAVAILABLE");
        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(started.getBody()).isNotNull();
        assertThat(started.getBody().code()).isEqualTo("SHOW_ALREADY_STARTED");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).isNotNull();
        assertThat(missing.getBody().code()).isEqualTo("HOLD_NOT_FOUND");
    }

    @Test
    void bookingErrorsUseStandardProblemCodes() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/holds/00000000-0000-0000-0000-000000000001/booking"
        );

        var expired = handler.handleHoldExpired(new HoldExpiredException(), request);
        var ownership = handler.handleHoldNoLongerOwnsSeats(
                new HoldNoLongerOwnsSeatsException(),
                request
        );
        var missing = handler.handleBookingNotFound(new BookingNotFoundException(), request);
        var bookingOwnership = handler.handleBookingNoLongerOwnsSeats(
                new BookingNoLongerOwnsSeatsException(),
                request
        );

        assertThat(expired.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(expired.getBody()).isNotNull();
        assertThat(expired.getBody().code()).isEqualTo("HOLD_EXPIRED");
        assertThat(ownership.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ownership.getBody()).isNotNull();
        assertThat(ownership.getBody().code()).isEqualTo("HOLD_NO_LONGER_OWNS_SEATS");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).isNotNull();
        assertThat(missing.getBody().code()).isEqualTo("BOOKING_NOT_FOUND");
        assertThat(bookingOwnership.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(bookingOwnership.getBody()).isNotNull();
        assertThat(bookingOwnership.getBody().code()).isEqualTo("BOOKING_NO_LONGER_OWNS_SEATS");
    }
}
