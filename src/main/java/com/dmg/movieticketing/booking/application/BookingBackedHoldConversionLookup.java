package com.dmg.movieticketing.booking.application;

import com.dmg.movieticketing.booking.domain.BookingRepository;
import com.dmg.movieticketing.hold.application.HoldConversionLookup;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Synchronous PostgreSQL-backed implementation of the hold conversion lookup.
 * It participates in the caller's existing transaction and opens no transaction of its own.
 */
@Component
public class BookingBackedHoldConversionLookup implements HoldConversionLookup {

    private final BookingRepository bookingRepository;

    public BookingBackedHoldConversionLookup(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public boolean isConverted(UUID holdId) {
        return bookingRepository.existsBySourceHoldId(holdId);
    }
}
