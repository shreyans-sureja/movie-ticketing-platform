package com.dmg.movieticketing.booking.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingItemRepository extends JpaRepository<BookingItem, BookingItemId> {

    @EntityGraph(attributePaths = "showSeat")
    @Query("""
            SELECT item
            FROM BookingItem item
            WHERE item.booking.id = :bookingId
            ORDER BY item.showSeat.rowLabel ASC, item.showSeat.seatNumber ASC, item.showSeat.id ASC
            """)
    List<BookingItem> findAllForBooking(@Param("bookingId") UUID bookingId);
}
