package com.dmg.movieticketing.hold.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {

    @EntityGraph(attributePaths = "show")
    Optional<SeatHold> findByIdAndCustomerAccountId(UUID id, UUID customerAccountId);

    /** Locks an owner-scoped hold to serialize concurrent confirmation attempts. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT hold
            FROM SeatHold hold
            WHERE hold.id = :id
              AND hold.customerAccountId = :customerAccountId
            """)
    Optional<SeatHold> findOwnedForUpdate(
            @Param("id") UUID id,
            @Param("customerAccountId") UUID customerAccountId
    );
}
