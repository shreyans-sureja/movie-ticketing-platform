package com.dmg.movieticketing.booking.application;

import com.dmg.movieticketing.booking.domain.Booking;
import com.dmg.movieticketing.booking.domain.BookingItem;
import com.dmg.movieticketing.booking.domain.BookingItemRepository;
import com.dmg.movieticketing.booking.domain.BookingRepository;
import com.dmg.movieticketing.hold.application.HoldNotFoundException;
import com.dmg.movieticketing.hold.application.ShowAlreadyStartedException;
import com.dmg.movieticketing.hold.domain.SeatHold;
import com.dmg.movieticketing.hold.domain.SeatHoldRepository;
import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.show.domain.ShowSeatAvailability;
import com.dmg.movieticketing.show.domain.ShowSeatRepository;
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

@Service
public class BookingService {

    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final Clock clock;

    public BookingService(
            SeatHoldRepository seatHoldRepository,
            ShowSeatRepository showSeatRepository,
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            Clock clock
    ) {
        this.seatHoldRepository = seatHoldRepository;
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.clock = clock;
    }

    @Transactional
    public BookingConfirmationResult confirm(UUID customerAccountId, UUID holdId) {
        SeatHold hold = seatHoldRepository.findOwnedForUpdate(holdId, customerAccountId)
                .orElseThrow(HoldNotFoundException::new);

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
        bookingItemRepository.saveAllAndFlush(items);

        seats.forEach(seat -> seat.confirmBooking(booking.getId()));
        showSeatRepository.saveAllAndFlush(seats);

        return new BookingConfirmationResult(
                new BookingDetails(booking, orderForResponse(items)),
                true
        );
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

    private List<BookingItem> orderForResponse(List<BookingItem> items) {
        return items.stream()
                .sorted(Comparator.comparing((BookingItem item) -> item.getShowSeat().getRowLabel())
                        .thenComparingInt(item -> item.getShowSeat().getSeatNumber())
                        .thenComparing(item -> item.getShowSeat().getId()))
                .toList();
    }
}
