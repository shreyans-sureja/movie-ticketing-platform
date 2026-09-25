package com.dmg.movieticketing.booking.api;

import com.dmg.movieticketing.booking.application.BookingHistoryItem;
import com.dmg.movieticketing.booking.domain.BookingStatus;
import com.dmg.movieticketing.show.api.MoneyResponse;

import java.time.Instant;
import java.util.UUID;

public record BookingSummaryResponse(
        UUID id,
        UUID showId,
        BookingStatus status,
        Instant confirmedAt,
        MoneyResponse totalPrice,
        long seatCount
) {

    static BookingSummaryResponse from(BookingHistoryItem item) {
        return new BookingSummaryResponse(
                item.id(),
                item.showId(),
                item.status(),
                item.confirmedAt(),
                new MoneyResponse(item.totalAmount(), item.currency()),
                item.seatCount()
        );
    }
}
