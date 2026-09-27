package com.dmg.movieticketing.notification.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedNotification(
        UUID bookingId,
        UUID customerAccountId,
        UUID showId,
        Instant confirmedAt,
        BigDecimal totalAmount,
        String currency,
        int seatCount
) implements NotificationMessage {

    @Override
    public NotificationType type() {
        return NotificationType.BOOKING_CONFIRMED;
    }
}
