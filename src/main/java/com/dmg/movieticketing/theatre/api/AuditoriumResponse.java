package com.dmg.movieticketing.theatre.api;

import com.dmg.movieticketing.theatre.application.AuditoriumDetails;

import java.time.Instant;
import java.util.UUID;

public record AuditoriumResponse(
        UUID id,
        UUID theatreId,
        String name,
        long seatCount,
        Instant createdAt
) {

    public static AuditoriumResponse from(AuditoriumDetails auditorium) {
        return new AuditoriumResponse(
                auditorium.id(),
                auditorium.theatreId(),
                auditorium.name(),
                auditorium.seatCount(),
                auditorium.createdAt()
        );
    }
}
