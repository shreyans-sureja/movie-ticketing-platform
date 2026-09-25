package com.dmg.movieticketing.booking.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record BookingListResponse(
        List<BookingSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static BookingListResponse from(Page<BookingSummaryResponse> page) {
        return new BookingListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
