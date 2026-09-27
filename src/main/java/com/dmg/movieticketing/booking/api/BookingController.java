package com.dmg.movieticketing.booking.api;

import com.dmg.movieticketing.booking.application.BookingService;
import com.dmg.movieticketing.identity.security.AccountPrincipal;
import com.dmg.movieticketing.theatre.application.DomainValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
public class BookingController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/api/v1/holds/{holdId}/booking")
    public ResponseEntity<BookingResponse> confirm(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID holdId
    ) {
        var result = bookingService.confirm(principal.accountId(), holdId);
        BookingResponse response = BookingResponse.from(result.details());
        if (!result.created()) {
            return ResponseEntity.ok(response);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/bookings/{bookingId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/v1/bookings/{bookingId}")
    public BookingResponse getOwnedBooking(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID bookingId
    ) {
        return BookingResponse.from(bookingService.getOwnedBooking(principal.accountId(), bookingId));
    }

    @PostMapping("/api/v1/bookings/{bookingId}/cancellation")
    public BookingResponse cancel(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID bookingId
    ) {
        return BookingResponse.from(bookingService.cancel(principal.accountId(), bookingId));
    }

    @GetMapping("/api/v1/bookings")
    public BookingListResponse listHistory(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        return BookingListResponse.from(
                bookingService.listHistory(principal.accountId(), PageRequest.of(page, size))
                        .map(BookingSummaryResponse::from)
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new DomainValidationException("page", "PAGE_MIN", "Page must be zero or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainValidationException("size", "SIZE_RANGE", "Size must be between 1 and 100.");
        }
    }
}
