package com.dmg.movieticketing.booking.application;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException() {
        super("Booking was not found.");
    }
}
