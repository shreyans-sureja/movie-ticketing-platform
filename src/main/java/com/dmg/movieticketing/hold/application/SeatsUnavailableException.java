package com.dmg.movieticketing.hold.application;

public class SeatsUnavailableException extends RuntimeException {

    public SeatsUnavailableException() {
        super("One or more requested seats are unavailable. No seats were held.");
    }
}
