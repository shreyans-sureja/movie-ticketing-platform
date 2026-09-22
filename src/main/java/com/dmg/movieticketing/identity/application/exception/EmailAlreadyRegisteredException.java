package com.dmg.movieticketing.identity.application.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("An account already exists for this email address.");
    }

    public EmailAlreadyRegisteredException(Throwable cause) {
        super("An account already exists for this email address.", cause);
    }
}

