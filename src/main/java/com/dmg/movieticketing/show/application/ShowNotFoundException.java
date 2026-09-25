package com.dmg.movieticketing.show.application;

public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException() {
        super("The requested show was not found.");
    }
}
