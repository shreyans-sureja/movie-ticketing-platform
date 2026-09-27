package com.dmg.movieticketing.notification.application;

import com.dmg.movieticketing.booking.application.BookingCancelledEvent;
import com.dmg.movieticketing.booking.application.BookingConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant CONFIRMED_AT = Instant.parse("2026-09-27T10:00:00Z");
    private static final Instant CANCELLED_AT = Instant.parse("2026-09-27T10:10:00Z");

    @Mock
    private NotificationSender notificationSender;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationSender);
    }

    @Test
    void mapsConfirmedEventToChannelNeutralMessage() {
        UUID bookingId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();

        service.send(new BookingConfirmedEvent(
                bookingId,
                customerId,
                showId,
                CONFIRMED_AT,
                new BigDecimal("500.00"),
                "INR",
                2
        ));

        ArgumentCaptor<NotificationMessage> message = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationSender).send(message.capture());
        assertThat(message.getValue()).isInstanceOfSatisfying(
                BookingConfirmedNotification.class,
                notification -> {
                    assertThat(notification.type()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
                    assertThat(notification.bookingId()).isEqualTo(bookingId);
                    assertThat(notification.customerAccountId()).isEqualTo(customerId);
                    assertThat(notification.showId()).isEqualTo(showId);
                    assertThat(notification.confirmedAt()).isEqualTo(CONFIRMED_AT);
                    assertThat(notification.totalAmount()).isEqualByComparingTo("500.00");
                    assertThat(notification.currency()).isEqualTo("INR");
                    assertThat(notification.seatCount()).isEqualTo(2);
                }
        );
    }

    @Test
    void mapsCancelledEventToChannelNeutralMessage() {
        UUID bookingId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();

        service.send(new BookingCancelledEvent(
                bookingId,
                customerId,
                showId,
                CONFIRMED_AT,
                CANCELLED_AT,
                new BigDecimal("500.00"),
                "INR",
                2
        ));

        ArgumentCaptor<NotificationMessage> message = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationSender).send(message.capture());
        assertThat(message.getValue()).isInstanceOfSatisfying(
                BookingCancelledNotification.class,
                notification -> {
                    assertThat(notification.type()).isEqualTo(NotificationType.BOOKING_CANCELLED);
                    assertThat(notification.bookingId()).isEqualTo(bookingId);
                    assertThat(notification.customerAccountId()).isEqualTo(customerId);
                    assertThat(notification.showId()).isEqualTo(showId);
                    assertThat(notification.confirmedAt()).isEqualTo(CONFIRMED_AT);
                    assertThat(notification.cancelledAt()).isEqualTo(CANCELLED_AT);
                    assertThat(notification.totalAmount()).isEqualByComparingTo("500.00");
                    assertThat(notification.currency()).isEqualTo("INR");
                    assertThat(notification.seatCount()).isEqualTo(2);
                }
        );
    }

    @Test
    void containsSenderFailuresForBothLifecycleEvents() {
        doThrow(new IllegalStateException("provider unavailable"))
                .when(notificationSender)
                .send(any());

        assertThatCode(() -> service.send(confirmedEvent())).doesNotThrowAnyException();
        assertThatCode(() -> service.send(cancelledEvent())).doesNotThrowAnyException();
    }

    private BookingConfirmedEvent confirmedEvent() {
        return new BookingConfirmedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                CONFIRMED_AT,
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
                CONFIRMED_AT,
                CANCELLED_AT,
                new BigDecimal("250.00"),
                "INR",
                1
        );
    }
}
