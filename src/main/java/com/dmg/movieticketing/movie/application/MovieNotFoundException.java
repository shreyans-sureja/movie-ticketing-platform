package com.dmg.movieticketing.movie.application;

public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException() {
        super("The requested movie was not found.");
    }
}
