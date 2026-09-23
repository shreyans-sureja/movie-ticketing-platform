package com.dmg.movieticketing.theatre.application;

public class SeatConflictException extends RuntimeException {

    public SeatConflictException() {
        super("One or more physical seats already exist in the requested range.");
    }

    public SeatConflictException(Throwable cause) {
        super("One or more physical seats already exist in the requested range.", cause);
    }
}
