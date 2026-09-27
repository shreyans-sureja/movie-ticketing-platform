package com.dmg.movieticketing.notification.application;

import com.dmg.movieticketing.booking.application.BookingCancelledEvent;
import com.dmg.movieticketing.booking.application.BookingConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationSender notificationSender;

    public NotificationService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void send(BookingConfirmedEvent event) {
        try {
            notificationSender.send(new BookingConfirmedNotification(
                    event.bookingId(),
                    event.customerAccountId(),
                    event.showId(),
                    event.confirmedAt(),
                    event.totalAmount(),
                    event.currency(),
                    event.seatCount()
            ));
        } catch (RuntimeException exception) {
            logFailure(NotificationType.BOOKING_CONFIRMED, event.bookingId(), exception);
        }
    }

    public void send(BookingCancelledEvent event) {
        try {
            notificationSender.send(new BookingCancelledNotification(
                    event.bookingId(),
                    event.customerAccountId(),
                    event.showId(),
                    event.confirmedAt(),
                    event.cancelledAt(),
                    event.totalAmount(),
                    event.currency(),
                    event.seatCount()
            ));
        } catch (RuntimeException exception) {
            logFailure(NotificationType.BOOKING_CANCELLED, event.bookingId(), exception);
        }
    }

    private void logFailure(NotificationType type, UUID bookingId, RuntimeException exception) {
        LOGGER.atWarn()
                .addKeyValue("event", "booking_notification_failed")
                .addKeyValue("notificationType", type)
                .addKeyValue("bookingId", bookingId)
                .addKeyValue("provider", "configured")
                .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                .setCause(exception)
                .log("Booking notification delivery failed after commit.");
    }
}
