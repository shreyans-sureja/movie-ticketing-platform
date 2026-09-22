package com.dmg.movieticketing.identity.api;

import com.dmg.movieticketing.identity.application.RegistrationResult;
import com.dmg.movieticketing.identity.domain.AccountRole;
import com.dmg.movieticketing.identity.domain.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String email,
        AccountRole role,
        AccountStatus status,
        Instant createdAt
) {
    public static AccountResponse from(RegistrationResult result) {
        return new AccountResponse(
                result.id(),
                result.email(),
                result.role(),
                result.status(),
                result.createdAt()
        );
    }
}

