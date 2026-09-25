package com.dmg.movieticketing.hold.application;

public class HoldNotFoundException extends RuntimeException {

    public HoldNotFoundException() {
        super("Hold was not found.");
    }
}
