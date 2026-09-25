package com.dmg.movieticketing.hold.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeatHoldItemRepository extends JpaRepository<SeatHoldItem, SeatHoldItemId> {

    @EntityGraph(attributePaths = "showSeat")
    @Query("""
            SELECT item
            FROM SeatHoldItem item
            WHERE item.hold.id = :holdId
            ORDER BY item.showSeat.rowLabel ASC, item.showSeat.seatNumber ASC, item.showSeat.id ASC
            """)
    List<SeatHoldItem> findAllForHold(@Param("holdId") UUID holdId);
}
