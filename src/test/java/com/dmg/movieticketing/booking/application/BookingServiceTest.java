package com.dmg.movieticketing.booking.application;

import com.dmg.movieticketing.booking.domain.Booking;
import com.dmg.movieticketing.booking.domain.BookingItem;
import com.dmg.movieticketing.booking.domain.BookingItemRepository;
import com.dmg.movieticketing.booking.domain.BookingRepository;
import com.dmg.movieticketing.booking.domain.BookingStatus;
import com.dmg.movieticketing.hold.domain.SeatHold;
import com.dmg.movieticketing.hold.domain.SeatHoldRepository;
import com.dmg.movieticketing.show.domain.MovieShow;
import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.show.domain.ShowSeatAvailability;
import com.dmg.movieticketing.show.domain.ShowSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingItemRepository bookingItemRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(
                seatHoldRepository,
                showSeatRepository,
                bookingRepository,
                bookingItemRepository,
                CLOCK,
                eventPublisher
        );
    }

    @Test
    void createsBookingFlushesItemsThenUpdatesEverySeat() {
        UUID customerId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();
        SeatHold hold = org.mockito.Mockito.mock(SeatHold.class);
        MovieShow show = org.mockito.Mockito.mock(MovieShow.class);
        ShowSeat firstSeat = ownedSeat(show, holdId, "250.00");
        ShowSeat secondSeat = ownedSeat(show, holdId, "400.00");

        when(hold.getId()).thenReturn(holdId);
        when(hold.getExpiresAt()).thenReturn(NOW.plusSeconds(60));
        when(hold.getShow()).thenReturn(show);
        when(show.getId()).thenReturn(showId);
        when(show.getStartsAt()).thenReturn(NOW.plusSeconds(3600));
        when(show.getCurrency()).thenReturn("INR");
        when(seatHoldRepository.findOwnedForUpdate(holdId, customerId)).thenReturn(Optional.of(hold));
        when(bookingRepository.findBySourceHoldId(holdId)).thenReturn(Optional.empty());
        when(showSeatRepository.findAllForBookingUpdate(holdId)).thenReturn(List.of(firstSeat, secondSeat));

        BookingConfirmationResult result = service.confirm(customerId, holdId);

        assertThat(result.created()).isTrue();
        assertThat(result.details().booking().getTotalAmount()).isEqualByComparingTo("650.00");
        assertThat(result.details().items()).hasSize(2);
        InOrder writes = inOrder(
                bookingRepository,
                bookingItemRepository,
                showSeatRepository,
                eventPublisher
        );
        writes.verify(bookingRepository).save(any(Booking.class));
        writes.verify(bookingItemRepository).saveAllAndFlush(anyList());
        writes.verify(showSeatRepository).saveAllAndFlush(List.of(firstSeat, secondSeat));
        ArgumentCaptor<BookingConfirmedEvent> event = ArgumentCaptor.forClass(BookingConfirmedEvent.class);
        writes.verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().bookingId()).isEqualTo(result.details().booking().getId());
        assertThat(event.getValue().customerAccountId()).isEqualTo(customerId);
        assertThat(event.getValue().showId()).isEqualTo(showId);
        assertThat(event.getValue().confirmedAt()).isEqualTo(NOW);
        assertThat(event.getValue().totalAmount()).isEqualByComparingTo("650.00");
        assertThat(event.getValue().currency()).isEqualTo("INR");
        assertThat(event.getValue().seatCount()).isEqualTo(2);
        verify(firstSeat).confirmBooking(result.details().booking().getId());
        verify(secondSeat).confirmBooking(result.details().booking().getId());
    }

    @Test
    void duplicateConfirmationReturnsExistingBookingBeforeExpiryCheckOrSeatLock() {
        UUID customerId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        SeatHold hold = org.mockito.Mockito.mock(SeatHold.class);
        Booking booking = org.mockito.Mockito.mock(Booking.class);

        when(booking.getId()).thenReturn(UUID.randomUUID());
        when(seatHoldRepository.findOwnedForUpdate(holdId, customerId)).thenReturn(Optional.of(hold));
        when(bookingRepository.findBySourceHoldId(holdId)).thenReturn(Optional.of(booking));
        when(bookingItemRepository.findAllForBooking(booking.getId())).thenReturn(List.of());

        BookingConfirmationResult result = service.confirm(customerId, holdId);

        assertThat(result.created()).isFalse();
        assertThat(result.details().booking()).isSameAs(booking);
        verifyNoInteractions(showSeatRepository);
        verifyNoInteractions(eventPublisher);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void expirationAtExactBoundaryRejectsBeforeWrites() {
        UUID customerId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        SeatHold hold = org.mockito.Mockito.mock(SeatHold.class);

        when(hold.getExpiresAt()).thenReturn(NOW);
        when(seatHoldRepository.findOwnedForUpdate(holdId, customerId)).thenReturn(Optional.of(hold));
        when(bookingRepository.findBySourceHoldId(holdId)).thenReturn(Optional.empty());
        when(showSeatRepository.findAllForBookingUpdate(holdId)).thenReturn(List.of(
                org.mockito.Mockito.mock(ShowSeat.class)
        ));

        assertThatThrownBy(() -> service.confirm(customerId, holdId))
                .isInstanceOf(HoldExpiredException.class);

        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(bookingItemRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void mismatchedSeatOwnershipRejectsCompleteConfirmation() {
        UUID customerId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        SeatHold hold = org.mockito.Mockito.mock(SeatHold.class);
        MovieShow show = org.mockito.Mockito.mock(MovieShow.class);
        ShowSeat seat = org.mockito.Mockito.mock(ShowSeat.class);

        when(hold.getId()).thenReturn(holdId);
        when(hold.getExpiresAt()).thenReturn(NOW.plusSeconds(60));
        when(hold.getShow()).thenReturn(show);
        when(show.getStartsAt()).thenReturn(NOW.plusSeconds(3600));
        when(show.getId()).thenReturn(UUID.randomUUID());
        when(seat.getShow()).thenReturn(show);
        when(seat.getAvailabilityStatus()).thenReturn(ShowSeatAvailability.HELD);
        when(seat.getCurrentHoldId()).thenReturn(UUID.randomUUID());
        when(seatHoldRepository.findOwnedForUpdate(holdId, customerId)).thenReturn(Optional.of(hold));
        when(bookingRepository.findBySourceHoldId(holdId)).thenReturn(Optional.empty());
        when(showSeatRepository.findAllForBookingUpdate(holdId)).thenReturn(List.of(seat));

        assertThatThrownBy(() -> service.confirm(customerId, holdId))
                .isInstanceOf(HoldNoLongerOwnsSeatsException.class);

        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(bookingItemRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void cancelsBookingAndReleasesCompleteSeatSet() {
        UUID customerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();
        Booking booking = Booking.confirm(
                bookingId,
                UUID.randomUUID(),
                showId,
                customerId,
                new BigDecimal("650.00"),
                "INR",
                NOW.minusSeconds(60)
        );
        MovieShow show = org.mockito.Mockito.mock(MovieShow.class);
        ShowSeat firstSeat = bookedSeat(show, bookingId);
        ShowSeat secondSeat = bookedSeat(show, bookingId);

        when(show.getId()).thenReturn(showId);
        when(show.getStartsAt()).thenReturn(NOW.plusSeconds(3600));
        when(bookingRepository.findOwnedForUpdate(bookingId, customerId)).thenReturn(Optional.of(booking));
        when(bookingItemRepository.countForBooking(bookingId)).thenReturn(2L);
        when(showSeatRepository.findAllForCancellationUpdate(bookingId))
                .thenReturn(List.of(firstSeat, secondSeat));
        when(bookingItemRepository.findAllForBooking(bookingId)).thenReturn(List.of());

        BookingDetails result = service.cancel(customerId, bookingId);

        assertThat(result.booking()).isSameAs(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isEqualTo(NOW);
        verify(firstSeat).releaseBooking();
        verify(secondSeat).releaseBooking();
        InOrder writes = inOrder(bookingRepository, showSeatRepository, eventPublisher);
        writes.verify(bookingRepository).save(booking);
        writes.verify(showSeatRepository).saveAllAndFlush(List.of(firstSeat, secondSeat));
        ArgumentCaptor<BookingCancelledEvent> event = ArgumentCaptor.forClass(BookingCancelledEvent.class);
        writes.verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(event.getValue().customerAccountId()).isEqualTo(customerId);
        assertThat(event.getValue().showId()).isEqualTo(showId);
        assertThat(event.getValue().confirmedAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(event.getValue().cancelledAt()).isEqualTo(NOW);
        assertThat(event.getValue().totalAmount()).isEqualByComparingTo("650.00");
        assertThat(event.getValue().currency()).isEqualTo("INR");
        assertThat(event.getValue().seatCount()).isEqualTo(2);
    }

    @Test
    void duplicateCancellationReturnsExistingBookingWithoutLockingSeatsAgain() {
        UUID customerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = org.mockito.Mockito.mock(Booking.class);

        when(booking.getId()).thenReturn(bookingId);
        when(booking.getStatus()).thenReturn(BookingStatus.CANCELLED);
        when(bookingRepository.findOwnedForUpdate(bookingId, customerId)).thenReturn(Optional.of(booking));
        when(bookingItemRepository.findAllForBooking(bookingId)).thenReturn(List.of());

        BookingDetails result = service.cancel(customerId, bookingId);

        assertThat(result.booking()).isSameAs(booking);
        verify(showSeatRepository, never()).findAllForCancellationUpdate(any());
        verify(bookingItemRepository, never()).countForBooking(any());
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void cancellationAtExactShowStartRejectsBeforeWrites() {
        UUID customerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = org.mockito.Mockito.mock(Booking.class);
        MovieShow show = org.mockito.Mockito.mock(MovieShow.class);
        ShowSeat seat = org.mockito.Mockito.mock(ShowSeat.class);

        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(seat.getShow()).thenReturn(show);
        when(show.getStartsAt()).thenReturn(NOW);
        when(bookingRepository.findOwnedForUpdate(bookingId, customerId)).thenReturn(Optional.of(booking));
        when(bookingItemRepository.countForBooking(bookingId)).thenReturn(1L);
        when(showSeatRepository.findAllForCancellationUpdate(bookingId)).thenReturn(List.of(seat));

        assertThatThrownBy(() -> service.cancel(customerId, bookingId))
                .isInstanceOf(com.dmg.movieticketing.hold.application.ShowAlreadyStartedException.class);

        verify(booking, never()).cancel(any());
        verify(seat, never()).releaseBooking();
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void incompleteCancellationSeatSetRejectsBeforeWrites() {
        UUID customerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = org.mockito.Mockito.mock(Booking.class);

        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(bookingRepository.findOwnedForUpdate(bookingId, customerId)).thenReturn(Optional.of(booking));
        when(bookingItemRepository.countForBooking(bookingId)).thenReturn(2L);
        when(showSeatRepository.findAllForCancellationUpdate(bookingId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.cancel(customerId, bookingId))
                .isInstanceOf(BookingNoLongerOwnsSeatsException.class);

        verify(booking, never()).cancel(any());
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    private ShowSeat ownedSeat(MovieShow show, UUID holdId, String price) {
        ShowSeat seat = org.mockito.Mockito.mock(ShowSeat.class);
        when(seat.getId()).thenReturn(UUID.randomUUID());
        when(seat.getRowLabel()).thenReturn("A");
        when(seat.getShow()).thenReturn(show);
        when(seat.getAvailabilityStatus()).thenReturn(ShowSeatAvailability.HELD);
        when(seat.getCurrentHoldId()).thenReturn(holdId);
        when(seat.getCurrency()).thenReturn("INR");
        when(seat.getPrice()).thenReturn(new BigDecimal(price));
        return seat;
    }

    private ShowSeat bookedSeat(MovieShow show, UUID bookingId) {
        ShowSeat seat = org.mockito.Mockito.mock(ShowSeat.class);
        when(seat.getShow()).thenReturn(show);
        when(seat.getAvailabilityStatus()).thenReturn(ShowSeatAvailability.BOOKED);
        when(seat.getCurrentBookingId()).thenReturn(bookingId);
        return seat;
    }
}
