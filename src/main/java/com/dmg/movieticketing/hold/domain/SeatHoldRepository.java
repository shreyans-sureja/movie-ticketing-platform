package com.dmg.movieticketing.hold.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {

    @EntityGraph(attributePaths = "show")
    Optional<SeatHold> findByIdAndCustomerAccountId(UUID id, UUID customerAccountId);
}
