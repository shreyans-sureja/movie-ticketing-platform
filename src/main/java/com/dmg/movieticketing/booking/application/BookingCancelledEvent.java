package com.dmg.movieticketing.booking.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingCancelledEvent(
        UUID bookingId,
        UUID customerAccountId,
        UUID showId,
        Instant confirmedAt,
        Instant cancelledAt,
        BigDecimal totalAmount,
        String currency,
        int seatCount
) {
}
