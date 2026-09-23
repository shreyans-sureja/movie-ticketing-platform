package com.dmg.movieticketing.theatre.application;

public class AuditoriumNameConflictException extends RuntimeException {

    public AuditoriumNameConflictException() {
        super("An auditorium with this name already exists in the theatre.");
    }

    public AuditoriumNameConflictException(Throwable cause) {
        super("An auditorium with this name already exists in the theatre.", cause);
    }
}
