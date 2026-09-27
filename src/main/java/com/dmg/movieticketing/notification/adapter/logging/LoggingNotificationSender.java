package com.dmg.movieticketing.notification.adapter.logging;

import com.dmg.movieticketing.notification.application.BookingCancelledNotification;
import com.dmg.movieticketing.notification.application.BookingConfirmedNotification;
import com.dmg.movieticketing.notification.application.NotificationMessage;
import com.dmg.movieticketing.notification.application.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development adapter that represents successful delivery by writing the message to the log.
 */
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
        LOGGER.info(
                "Booking lifecycle notification delivered: type={}, details={}",
                notification.type(),
                notification
        );
    }
}
