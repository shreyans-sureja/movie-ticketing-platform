package com.dmg.movieticketing.theatre.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhysicalSeatRepository extends JpaRepository<PhysicalSeat, UUID> {

    List<PhysicalSeat> findAllByAuditoriumIdOrderByRowLabelAscSeatNumberAscIdAsc(UUID auditoriumId);

    long countByAuditoriumId(UUID auditoriumId);

    boolean existsByAuditoriumIdAndRowLabelAndSeatNumberBetween(
            UUID auditoriumId,
            String rowLabel,
            int firstSeatNumber,
            int lastSeatNumber
    );
}
