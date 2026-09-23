package com.dmg.movieticketing.theatre.application;

public class TheatreNotFoundException extends RuntimeException {

    public TheatreNotFoundException() {
        super("Theatre was not found.");
    }
}
