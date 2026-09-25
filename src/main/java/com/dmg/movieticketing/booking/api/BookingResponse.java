package com.dmg.movieticketing.booking.api;

import com.dmg.movieticketing.booking.application.BookingDetails;
import com.dmg.movieticketing.booking.domain.BookingStatus;
import com.dmg.movieticketing.show.api.MoneyResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID holdId,
        UUID showId,
        BookingStatus status,
        Instant confirmedAt,
        MoneyResponse totalPrice,
        List<BookingSeatResponse> seats
) {

    static BookingResponse from(BookingDetails details) {
        var booking = details.booking();
        return new BookingResponse(
                booking.getId(),
                booking.getSourceHoldId(),
                booking.getShowId(),
                booking.getStatus(),
                booking.getConfirmedAt(),
                new MoneyResponse(booking.getTotalAmount(), booking.getCurrency()),
                details.items().stream()
                        .map(item -> BookingSeatResponse.from(item, booking.getCurrency()))
                        .toList()
        );
    }
}
