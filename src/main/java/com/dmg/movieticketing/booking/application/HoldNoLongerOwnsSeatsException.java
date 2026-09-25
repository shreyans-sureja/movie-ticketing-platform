package com.dmg.movieticketing.booking.application;

public class HoldNoLongerOwnsSeatsException extends RuntimeException {

    public HoldNoLongerOwnsSeatsException() {
        super("The seat hold no longer owns its complete seat set.");
    }
}
