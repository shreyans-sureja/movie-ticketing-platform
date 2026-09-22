package com.dmg.movieticketing.identity.security;

import com.dmg.movieticketing.identity.domain.AccountRole;

import java.util.UUID;

public record AccountPrincipal(
        UUID accountId,
        AccountRole role,
        String authenticationMethod
) {
}

