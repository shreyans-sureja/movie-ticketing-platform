package com.dmg.movieticketing.booking.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable fact published when a hold creates a booking for the first time.
 * Consumers must treat {@code (event type, bookingId)} as its logical identity.
 */
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
