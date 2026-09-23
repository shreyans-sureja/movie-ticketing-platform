package com.dmg.movieticketing.theatre.application;

import java.time.Instant;
import java.util.UUID;

public record AuditoriumDetails(
        UUID id,
        UUID theatreId,
        String name,
        long seatCount,
        Instant createdAt
) {
}
