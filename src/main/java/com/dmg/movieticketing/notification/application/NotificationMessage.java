package com.dmg.movieticketing.notification.application;

import java.math.BigDecimal;
import java.util.UUID;

/** Common, channel-neutral fields available to every notification adapter. */
public interface NotificationMessage {

    NotificationType type();

    UUID bookingId();

    UUID customerAccountId();

    UUID showId();

    BigDecimal totalAmount();

    String currency();

    int seatCount();
}
