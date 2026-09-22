package com.dmg.movieticketing.identity.application.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("The account no longer exists.");
    }
}
