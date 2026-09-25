package com.dmg.movieticketing.show.application;

public class AuditoriumHasNoSeatsException extends RuntimeException {

    public AuditoriumHasNoSeatsException() {
        super("A show cannot be scheduled before the auditorium has physical seats.");
    }
}
