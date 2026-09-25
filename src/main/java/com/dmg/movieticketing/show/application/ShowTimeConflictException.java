package com.dmg.movieticketing.show.application;

public class ShowTimeConflictException extends RuntimeException {

    public ShowTimeConflictException() {
        super("The auditorium already has a show during the requested time.");
    }
}
