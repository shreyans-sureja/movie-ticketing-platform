package com.dmg.movieticketing.show.api;

import com.dmg.movieticketing.theatre.domain.Auditorium;

import java.util.UUID;

public record ShowAuditoriumResponse(UUID id, String name) {

    public static ShowAuditoriumResponse from(Auditorium auditorium) {
        return new ShowAuditoriumResponse(auditorium.getId(), auditorium.getName());
    }
}
