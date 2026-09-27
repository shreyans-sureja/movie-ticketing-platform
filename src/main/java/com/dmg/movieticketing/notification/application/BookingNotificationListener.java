package com.dmg.movieticketing.notification.application;

import com.dmg.movieticketing.booking.application.BookingCancelledEvent;
import com.dmg.movieticketing.booking.application.BookingConfirmedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges committed booking lifecycle events to the notification boundary.
 * Fallback execution is disabled so events published without a transaction are ignored.
 */
@Component
public class BookingNotificationListener {

    private final NotificationService notificationService;

    public BookingNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Delivers a confirmation notification only after its booking transaction commits. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        notificationService.send(event);
    }

    /** Delivers a cancellation notification only after its booking transaction commits. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void onBookingCancelled(BookingCancelledEvent event) {
        notificationService.send(event);
    }
}
