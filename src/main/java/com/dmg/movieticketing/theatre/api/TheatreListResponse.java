package com.dmg.movieticketing.theatre.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record TheatreListResponse(
        List<TheatreResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static TheatreListResponse from(Page<TheatreResponse> page) {
        return new TheatreListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
