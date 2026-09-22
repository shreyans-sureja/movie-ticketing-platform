package com.dmg.movieticketing.identity.application;

import com.dmg.movieticketing.identity.domain.AccountRole;

import java.util.UUID;

public record SignInResult(
        String tokenType,
        String accessToken,
        long expiresIn,
        UUID accountId,
        AccountRole role
) {
}

