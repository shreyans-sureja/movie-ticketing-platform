package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.application.exception.PasswordPolicyViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void acceptsPasswordWithinBcryptByteLimit() {
        assertThatCode(() -> passwordPolicy.validateForBcrypt("a".repeat(72)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordThatBcryptWouldTruncate() {
        assertThatThrownBy(() -> passwordPolicy.validateForBcrypt("a".repeat(73)))
                .isInstanceOf(PasswordPolicyViolationException.class)
                .hasMessageContaining("72 UTF-8 bytes");
    }

    @Test
    void countsUtf8BytesRatherThanJavaCharacters() {
        assertThatThrownBy(() -> passwordPolicy.validateForBcrypt("€".repeat(25)))
                .isInstanceOf(PasswordPolicyViolationException.class);
    }
}

