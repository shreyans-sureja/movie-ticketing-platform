package com.dmg.movieticketing.identity.application.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(Throwable cause) {
        super("The email or password is invalid.", cause);
    }
}

