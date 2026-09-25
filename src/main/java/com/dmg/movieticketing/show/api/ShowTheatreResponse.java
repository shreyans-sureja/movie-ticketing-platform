package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.theatre.domain.Theatre;

import java.util.UUID;

public record ShowTheatreResponse(UUID id, String name) {

    public static ShowTheatreResponse from(Theatre theatre) {
        return new ShowTheatreResponse(theatre.getId(), theatre.getName());
    }
}
