package com.dmg.movieticketing.booking.application;

import com.dmg.movieticketing.booking.domain.BookingRepository;
import com.dmg.movieticketing.hold.application.HoldConversionLookup;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
