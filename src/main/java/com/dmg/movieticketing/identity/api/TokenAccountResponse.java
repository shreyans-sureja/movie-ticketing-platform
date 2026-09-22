package com.dmg.movieticketing.identity.api;

import com.dmg.movieticketing.identity.domain.AccountRole;

import java.util.UUID;

public record TokenAccountResponse(UUID id, AccountRole role) {
}

