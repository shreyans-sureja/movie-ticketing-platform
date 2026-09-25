package com.dmg.movieticketing.booking.application;

import com.dmg.movieticketing.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingHistoryItem(
        UUID id,
        UUID showId,
        BookingStatus status,
        Instant confirmedAt,
        BigDecimal totalAmount,
        String currency,
        Long seatCount
) {
}
