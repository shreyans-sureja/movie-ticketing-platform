package com.dmg.movieticketing.notification.application;

import com.dmg.movieticketing.booking.application.BookingCancelledEvent;
import com.dmg.movieticketing.booking.application.BookingConfirmedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingNotificationListener {

    private final NotificationService notificationService;

    public BookingNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        notificationService.send(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void onBookingCancelled(BookingCancelledEvent event) {
        notificationService.send(event);
    }
}
