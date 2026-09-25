package com.dmg.movieticketing.show.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record ShowListResponse(
        List<ShowSearchItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ShowListResponse from(Page<ShowSearchItemResponse> page) {
        return new ShowListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
