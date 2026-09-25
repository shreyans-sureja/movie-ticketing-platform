package com.dmg.movieticketing.movie.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record MovieListResponse(
        List<MovieSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static MovieListResponse from(Page<MovieSummaryResponse> page) {
        return new MovieListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
