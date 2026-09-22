package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.domain.AccountRole;
import com.dmg.movieticketing.identity.domain.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record RegistrationResult(
        UUID id,
        String email,
        AccountRole role,
        AccountStatus status,
        Instant createdAt
) {
}

