package com.dmg.movieticketing.booking.domain;

import com.dmg.movieticketing.booking.application.BookingHistoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findBySourceHoldId(UUID sourceHoldId);

    Optional<Booking> findByIdAndCustomerAccountId(UUID id, UUID customerAccountId);

    /** Locks an owner-scoped booking to serialize cancellation attempts. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT booking
            FROM Booking booking
            WHERE booking.id = :id
              AND booking.customerAccountId = :customerAccountId
            """)
    Optional<Booking> findOwnedForUpdate(
            @Param("id") UUID id,
            @Param("customerAccountId") UUID customerAccountId
    );

    boolean existsBySourceHoldId(UUID sourceHoldId);

    /** Projects paged history with seat counts in one grouped query. */
    @Query(
            value = """
            SELECT new com.dmg.movieticketing.booking.application.BookingHistoryItem(
                booking.id,
                booking.showId,
                booking.status,
                booking.confirmedAt,
                booking.cancelledAt,
                booking.totalAmount,
                booking.currency,
                COUNT(item)
            )
            FROM Booking booking
            JOIN BookingItem item ON item.booking = booking
            WHERE booking.customerAccountId = :customerAccountId
            GROUP BY
                booking.id,
                booking.showId,
                booking.status,
                booking.confirmedAt,
                booking.cancelledAt,
                booking.totalAmount,
                booking.currency
            ORDER BY booking.confirmedAt DESC, booking.id DESC
            """,
            countQuery = """
            SELECT COUNT(booking)
            FROM Booking booking
            WHERE booking.customerAccountId = :customerAccountId
            """
    )
    Page<BookingHistoryItem> findHistoryByCustomerAccountId(
            @Param("customerAccountId") UUID customerAccountId,
            Pageable pageable
    );
}
