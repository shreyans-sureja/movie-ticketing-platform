package com.dmg.movieticketing.identity.application.exception;

public class PasswordPolicyViolationException extends RuntimeException {

    public PasswordPolicyViolationException(String message) {
        super(message);
    }
}

