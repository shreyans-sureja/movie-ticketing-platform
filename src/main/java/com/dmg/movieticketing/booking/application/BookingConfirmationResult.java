package com.dmg.movieticketing.booking.application;

public record BookingConfirmationResult(
        BookingDetails details,
        boolean created
) {
}
