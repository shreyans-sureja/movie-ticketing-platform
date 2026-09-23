package com.dmg.movieticketing.theatre.application;

public class TheatreNameConflictException extends RuntimeException {

    public TheatreNameConflictException() {
        super("A theatre with this name already exists for the owner in the city.");
    }

    public TheatreNameConflictException(Throwable cause) {
        super("A theatre with this name already exists for the owner in the city.", cause);
    }
}
