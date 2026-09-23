package com.dmg.movieticketing.theatre.application;

public class AuditoriumNotFoundException extends RuntimeException {

    public AuditoriumNotFoundException() {
        super("Auditorium was not found.");
    }
}
