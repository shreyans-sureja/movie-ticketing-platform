package com.dmg.movieticketing.booking.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable fact published for the first successful transition of a booking to cancelled.
 */
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
