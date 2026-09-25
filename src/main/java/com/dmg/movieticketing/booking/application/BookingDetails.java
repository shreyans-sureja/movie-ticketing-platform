package com.dmg.movieticketing.booking.application;

import com.dmg.movieticketing.booking.domain.Booking;
import com.dmg.movieticketing.booking.domain.BookingItem;

import java.util.List;

public record BookingDetails(
        Booking booking,
        List<BookingItem> items
) {
}
