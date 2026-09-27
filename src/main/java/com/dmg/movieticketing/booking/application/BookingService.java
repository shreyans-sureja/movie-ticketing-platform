package com.dmg.movieticketing.booking.application;

import com.dmg.movieticketing.booking.domain.Booking;
import com.dmg.movieticketing.booking.domain.BookingItem;
import com.dmg.movieticketing.booking.domain.BookingItemRepository;
import com.dmg.movieticketing.booking.domain.BookingRepository;
import com.dmg.movieticketing.booking.domain.BookingStatus;
import com.dmg.movieticketing.hold.application.HoldNotFoundException;
import com.dmg.movieticketing.hold.application.ShowAlreadyStartedException;
import com.dmg.movieticketing.hold.domain.SeatHold;
import com.dmg.movieticketing.hold.domain.SeatHoldRepository;
import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.show.domain.ShowSeatAvailability;
import com.dmg.movieticketing.show.domain.ShowSeatRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Coordinates customer-owned booking confirmation, cancellation, and reads.
 * Database row locks and constraints provide the concurrency guarantees; this
 * service deliberately does not use JVM-local synchronization.
 */
@Service
public class BookingService {

    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(
            SeatHoldRepository seatHoldRepository,
            ShowSeatRepository showSeatRepository,
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            Clock clock,
            ApplicationEventPublisher eventPublisher
    ) {
        this.seatHoldRepository = seatHoldRepository;
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Atomically converts an active hold into a booking, or returns the booking
     * already created from that hold when the request is retried.
     */
    @Transactional
    public BookingConfirmationResult confirm(UUID customerAccountId, UUID holdId) {
        // The hold is the first lock in every confirmation attempt, serializing retries for one hold.
        SeatHold hold = seatHoldRepository.findOwnedForUpdate(holdId, customerAccountId)
                .orElseThrow(HoldNotFoundException::new);

        // Check idempotency only after acquiring the hold lock so concurrent requests agree on one result.
        Booking existing = bookingRepository.findBySourceHoldId(holdId).orElse(null);
        if (existing != null) {
            return new BookingConfirmationResult(loadDetails(existing), false);
        }

        List<ShowSeat> seats = showSeatRepository.findAllForBookingUpdate(holdId);
        Instant confirmedAt = clock.instant();

        if (!confirmedAt.isBefore(hold.getExpiresAt())) {
            throw new HoldExpiredException();
        }
        if (!confirmedAt.isBefore(hold.getShow().getStartsAt())) {
            throw new ShowAlreadyStartedException();
        }
        if (seats.isEmpty() || seats.stream().anyMatch(seat -> !isOwnedBy(seat, hold))) {
            throw new HoldNoLongerOwnsSeatsException();
        }

        String currency = hold.getShow().getCurrency();
        if (seats.stream().anyMatch(seat -> !currency.equals(seat.getCurrency()))) {
            throw new IllegalStateException("Hold seats must use the show's currency.");
        }
        BigDecimal totalAmount = seats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Booking booking = Booking.confirm(
                UUID.randomUUID(),
                hold.getId(),
                hold.getShow().getId(),
                customerAccountId,
                totalAmount,
                currency,
                confirmedAt
        );
        bookingRepository.save(booking);

        List<BookingItem> items = seats.stream()
                .map(seat -> BookingItem.create(booking, seat))
                .toList();
        // Items must exist before show_seat.current_booking_id is set because the database FK references them.
        bookingItemRepository.saveAllAndFlush(items);

        seats.forEach(seat -> seat.confirmBooking(booking.getId()));
        showSeatRepository.saveAllAndFlush(seats);

        // Publication occurs inside the transaction; the listener intentionally runs only after commit.
        eventPublisher.publishEvent(new BookingConfirmedEvent(
                booking.getId(),
                booking.getCustomerAccountId(),
                booking.getShowId(),
                booking.getConfirmedAt(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                items.size()
        ));

        return new BookingConfirmationResult(
                new BookingDetails(booking, orderForResponse(items)),
                true
        );
    }

    /**
     * Cancels a complete booking before show start and releases all of its seats
     * in the same transaction. Replaying an already completed cancellation is safe.
     */
    @Transactional
    public BookingDetails cancel(UUID customerAccountId, UUID bookingId) {
        Booking booking = bookingRepository.findOwnedForUpdate(bookingId, customerAccountId)
                .orElseThrow(BookingNotFoundException::new);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return loadDetails(booking);
        }

        long itemCount = bookingItemRepository.countForBooking(bookingId);
        List<ShowSeat> seats = showSeatRepository.findAllForCancellationUpdate(bookingId);
        // Never partially release a booking if its current seat ownership is inconsistent.
        if (itemCount == 0 || seats.size() != itemCount) {
            throw new BookingNoLongerOwnsSeatsException();
        }

        Instant cancelledAt = clock.instant();
        if (!cancelledAt.isBefore(seats.getFirst().getShow().getStartsAt())) {
            throw new ShowAlreadyStartedException();
        }
        if (seats.stream().anyMatch(seat -> !isOwnedBy(seat, booking))) {
            throw new BookingNoLongerOwnsSeatsException();
        }

        booking.cancel(cancelledAt);
        seats.forEach(ShowSeat::releaseBooking);
        bookingRepository.save(booking);
        showSeatRepository.saveAllAndFlush(seats);

        // Idempotent retries return above, so only the state-changing request publishes this event.
        eventPublisher.publishEvent(new BookingCancelledEvent(
                booking.getId(),
                booking.getCustomerAccountId(),
                booking.getShowId(),
                booking.getConfirmedAt(),
                booking.getCancelledAt(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                seats.size()
        ));

        return loadDetails(booking);
    }

    @Transactional(readOnly = true)
    public BookingDetails getOwnedBooking(UUID customerAccountId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndCustomerAccountId(bookingId, customerAccountId)
                .orElseThrow(BookingNotFoundException::new);
        return loadDetails(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingHistoryItem> listHistory(UUID customerAccountId, Pageable pageable) {
        return bookingRepository.findHistoryByCustomerAccountId(customerAccountId, pageable);
    }

    private BookingDetails loadDetails(Booking booking) {
        return new BookingDetails(
                booking,
                List.copyOf(bookingItemRepository.findAllForBooking(booking.getId()))
        );
    }

    private boolean isOwnedBy(ShowSeat seat, SeatHold hold) {
        return seat.getShow().getId().equals(hold.getShow().getId())
                && seat.getAvailabilityStatus() == ShowSeatAvailability.HELD
                && hold.getId().equals(seat.getCurrentHoldId());
    }

    private boolean isOwnedBy(ShowSeat seat, Booking booking) {
        return seat.getShow().getId().equals(booking.getShowId())
                && seat.getAvailabilityStatus() == ShowSeatAvailability.BOOKED
                && booking.getId().equals(seat.getCurrentBookingId())
                && seat.getCurrentHoldId() == null;
    }

    private List<BookingItem> orderForResponse(List<BookingItem> items) {
        return items.stream()
                .sorted(Comparator.comparing((BookingItem item) -> item.getShowSeat().getRowLabel())
                        .thenComparingInt(item -> item.getShowSeat().getSeatNumber())
                        .thenComparing(item -> item.getShowSeat().getId()))
                .toList();
    }
}
