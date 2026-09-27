package com.dmg.movieticketing.notification.adapter.logging;

import com.dmg.movieticketing.notification.application.BookingCancelledNotification;
import com.dmg.movieticketing.notification.application.BookingConfirmedNotification;
import com.dmg.movieticketing.notification.application.NotificationMessage;
import com.dmg.movieticketing.notification.application.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.notifications",
        name = "provider",
        havingValue = "logging",
        matchIfMissing = true
)
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(NotificationMessage notification) {
        var log = LOGGER.atInfo()
                .addKeyValue("event", "booking_notification_sent")
                .addKeyValue("notificationType", notification.type())
                .addKeyValue("bookingId", notification.bookingId())
                .addKeyValue("customerAccountId", notification.customerAccountId())
                .addKeyValue("showId", notification.showId())
                .addKeyValue("totalAmount", notification.totalAmount())
                .addKeyValue("currency", notification.currency())
                .addKeyValue("seatCount", notification.seatCount())
                .addKeyValue("provider", "local-log");

        if (notification instanceof BookingConfirmedNotification confirmed) {
            log.addKeyValue("confirmedAt", confirmed.confirmedAt());
        } else if (notification instanceof BookingCancelledNotification cancelled) {
            log.addKeyValue("confirmedAt", cancelled.confirmedAt())
                    .addKeyValue("cancelledAt", cancelled.cancelledAt());
        }

        log.log("Booking lifecycle notification delivered to local logging adapter.");
    }
}
