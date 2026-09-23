package com.dmg.movieticketing.theatre.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuditoriumRequest(
        @NotBlank @Size(max = 100) String name
) {
}
