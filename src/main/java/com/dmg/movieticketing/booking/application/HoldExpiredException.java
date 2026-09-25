package com.dmg.movieticketing.booking.application;

public class HoldExpiredException extends RuntimeException {

    public HoldExpiredException() {
        super("The seat hold has expired.");
    }
}
