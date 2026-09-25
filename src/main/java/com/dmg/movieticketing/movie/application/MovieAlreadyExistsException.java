package com.dmg.movieticketing.movie.application;

public class MovieAlreadyExistsException extends RuntimeException {

    public MovieAlreadyExistsException() {
        super("A movie with the same title, language, and runtime already exists.");
    }

    public MovieAlreadyExistsException(Throwable cause) {
        super("A movie with the same title, language, and runtime already exists.", cause);
    }
}
