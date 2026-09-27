package com.dmg.movieticketing.notification.adapter.logging;

import com.dmg.movieticketing.notification.application.BookingCancelledNotification;
import com.dmg.movieticketing.notification.application.BookingConfirmedNotification;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingNotificationSenderTest {

    private final LoggingNotificationSender sender = new LoggingNotificationSender();

    @Test
    void logsBothBookingLifecycleNotificationTypes() {
        UUID bookingId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();
        Instant confirmedAt = Instant.parse("2026-09-27T10:00:00Z");

        assertThatCode(() -> sender.send(new BookingConfirmedNotification(
                bookingId,
                customerId,
                showId,
                confirmedAt,
                new BigDecimal("500.00"),
                "INR",
                2
        ))).doesNotThrowAnyException();

        assertThatCode(() -> sender.send(new BookingCancelledNotification(
                bookingId,
                customerId,
                showId,
                confirmedAt,
                Instant.parse("2026-09-27T10:10:00Z"),
                new BigDecimal("500.00"),
                "INR",
                2
        ))).doesNotThrowAnyException();
    }
}
