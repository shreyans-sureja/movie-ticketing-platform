package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.application.exception.PasswordPolicyViolationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PasswordPolicy {

    static final int BCRYPT_MAX_BYTES = 72;

    public void validateForBcrypt(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new PasswordPolicyViolationException(
                    "Password must not exceed 72 UTF-8 bytes when BCrypt is used."
            );
        }
    }
}

