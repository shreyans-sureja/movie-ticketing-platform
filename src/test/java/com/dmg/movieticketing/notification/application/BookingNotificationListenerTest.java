package com.dmg.movieticketing.notification.application;

import com.dmg.movieticketing.booking.application.BookingCancelledEvent;
import com.dmg.movieticketing.booking.application.BookingConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    private BookingNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new BookingNotificationListener(notificationService);
    }

    @Test
    void confirmedListenerUsesAfterCommitWithoutFallbackAndDelegates() throws Exception {
        BookingConfirmedEvent event = confirmedEvent();

        listener.onBookingConfirmed(event);

        verify(notificationService).send(event);
        assertAfterCommitWithoutFallback(
                BookingNotificationListener.class.getMethod(
                        "onBookingConfirmed",
                        BookingConfirmedEvent.class
                )
        );
    }

    @Test
    void cancelledListenerUsesAfterCommitWithoutFallbackAndDelegates() throws Exception {
        BookingCancelledEvent event = cancelledEvent();

        listener.onBookingCancelled(event);

        verify(notificationService).send(event);
        assertAfterCommitWithoutFallback(
                BookingNotificationListener.class.getMethod(
                        "onBookingCancelled",
                        BookingCancelledEvent.class
                )
        );
    }

    private void assertAfterCommitWithoutFallback(Method method) {
        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
    }

    private BookingConfirmedEvent confirmedEvent() {
        return new BookingConfirmedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-09-27T10:00:00Z"),
                new BigDecimal("250.00"),
                "INR",
                1
        );
    }

    private BookingCancelledEvent cancelledEvent() {
        return new BookingCancelledEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-09-27T10:00:00Z"),
                Instant.parse("2026-09-27T10:10:00Z"),
                new BigDecimal("250.00"),
                "INR",
                1
        );
    }
}
