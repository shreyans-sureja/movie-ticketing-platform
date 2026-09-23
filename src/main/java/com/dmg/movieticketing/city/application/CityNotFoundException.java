package com.dmg.movieticketing.city.application;

public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException() {
        super("City was not found.");
    }
}
