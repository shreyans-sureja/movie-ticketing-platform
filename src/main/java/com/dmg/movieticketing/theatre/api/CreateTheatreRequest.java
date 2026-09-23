package com.dmg.movieticketing.theatre.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTheatreRequest(
        @NotNull @Positive Long cityId,
        @NotBlank @Size(min = 2, max = 150) String name,
        @NotBlank @Size(min = 5, max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String postalCode
) {
}
