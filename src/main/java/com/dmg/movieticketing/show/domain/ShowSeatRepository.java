package com.dmg.movieticketing.show.domain;

import com.dmg.movieticketing.show.application.ShowSeatAvailabilityData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    @Query("""
            SELECT new com.dmg.movieticketing.show.application.ShowSeatAvailabilityData(
                showSeat,
                currentHold.expiresAt
            )
            FROM ShowSeat showSeat
            LEFT JOIN SeatHold currentHold ON currentHold.id = showSeat.currentHoldId
            WHERE showSeat.show.id = :showId
            ORDER BY showSeat.rowLabel ASC, showSeat.seatNumber ASC, showSeat.id ASC
            """)
    List<ShowSeatAvailabilityData> findAllWithCurrentHoldExpiry(@Param("showId") UUID showId);

    @Query(value = """
            SELECT *
            FROM show_seat
            WHERE show_id = :showId
              AND id IN (:showSeatIds)
            ORDER BY id
            FOR UPDATE
            """, nativeQuery = true)
    List<ShowSeat> findAllForHoldUpdate(
            @Param("showId") UUID showId,
            @Param("showSeatIds") List<UUID> showSeatIds
    );

    @Query(value = """
            SELECT show_seat.*
            FROM show_seat
            JOIN seat_hold_item ON seat_hold_item.show_seat_id = show_seat.id
            WHERE seat_hold_item.hold_id = :holdId
            ORDER BY show_seat.id
            FOR UPDATE OF show_seat
            """, nativeQuery = true)
    List<ShowSeat> findAllForBookingUpdate(@Param("holdId") UUID holdId);

    @Query(value = """
            SELECT show_seat.*
            FROM show_seat
            JOIN booking_item ON booking_item.show_seat_id = show_seat.id
            WHERE booking_item.booking_id = :bookingId
            ORDER BY show_seat.id
            FOR UPDATE OF show_seat
            """, nativeQuery = true)
    List<ShowSeat> findAllForCancellationUpdate(@Param("bookingId") UUID bookingId);

    long countByShowId(UUID showId);

    @Query("""
            SELECT COUNT(showSeat)
            FROM ShowSeat showSeat
            LEFT JOIN SeatHold currentHold ON currentHold.id = showSeat.currentHoldId
            WHERE showSeat.show.id = :showId
              AND (
                    showSeat.availabilityStatus = com.dmg.movieticketing.show.domain.ShowSeatAvailability.AVAILABLE
                    OR (
                        showSeat.availabilityStatus = com.dmg.movieticketing.show.domain.ShowSeatAvailability.HELD
                        AND currentHold.expiresAt <= :requestNow
                    )
              )
            """)
    long countEffectivelyAvailable(
            @Param("showId") UUID showId,
            @Param("requestNow") Instant requestNow
    );
}
