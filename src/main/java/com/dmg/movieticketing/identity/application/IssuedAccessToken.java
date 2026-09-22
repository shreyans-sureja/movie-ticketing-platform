package com.dmg.movieticketing.identity.application;

import java.time.Instant;

public record IssuedAccessToken(String value, Instant issuedAt, Instant expiresAt) {
}
