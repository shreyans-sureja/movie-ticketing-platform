package com.dmg.movieticketing.booking.application;

public class BookingNoLongerOwnsSeatsException extends RuntimeException {

    public BookingNoLongerOwnsSeatsException() {
        super("The booking no longer owns its complete seat set.");
    }
}
