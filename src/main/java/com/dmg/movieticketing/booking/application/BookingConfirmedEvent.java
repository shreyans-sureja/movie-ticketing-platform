package com.dmg.movieticketing.booking.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedEvent(
        UUID bookingId,
        UUID customerAccountId,
        UUID showId,
        Instant confirmedAt,
        BigDecimal totalAmount,
        String currency,
        int seatCount
) {
}
