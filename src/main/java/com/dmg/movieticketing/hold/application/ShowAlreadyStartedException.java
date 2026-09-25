package com.dmg.movieticketing.hold.application;

public class ShowAlreadyStartedException extends RuntimeException {

    public ShowAlreadyStartedException() {
        super("The show has already started. New holds are closed.");
    }
}
