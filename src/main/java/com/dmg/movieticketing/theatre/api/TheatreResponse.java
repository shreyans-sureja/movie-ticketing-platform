package com.dmg.movieticketing.theatre.api;

import com.dmg.movieticketing.city.api.CityResponse;
import com.dmg.movieticketing.theatre.domain.Theatre;

import java.time.Instant;
import java.util.UUID;

public record TheatreResponse(
        UUID id,
        CityResponse city,
        String name,
        String addressLine1,
        String addressLine2,
        String postalCode,
        Instant createdAt
) {

    public static TheatreResponse from(Theatre theatre) {
        return new TheatreResponse(
                theatre.getId(),
                CityResponse.from(theatre.getCity()),
                theatre.getName(),
                theatre.getAddressLine1(),
                theatre.getAddressLine2(),
                theatre.getPostalCode(),
                theatre.getCreatedAt()
        );
    }
}
