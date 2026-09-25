package com.dmg.movieticketing.movie.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMovieRequest(
        @NotBlank @Size(max = 200) String title,
        @Min(1) @Max(600) int durationMinutes,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2,10}") String languageCode
) {
}
