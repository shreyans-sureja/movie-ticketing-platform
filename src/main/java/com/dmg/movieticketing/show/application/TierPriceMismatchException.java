package com.dmg.movieticketing.show.application;

public class TierPriceMismatchException extends RuntimeException {

    public TierPriceMismatchException() {
        super("Tier prices must contain exactly one price for every tier used by the auditorium.");
    }
}
