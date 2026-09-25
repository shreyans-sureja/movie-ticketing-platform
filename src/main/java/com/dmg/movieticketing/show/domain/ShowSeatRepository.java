package com.dmg.movieticketing.show.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    List<ShowSeat> findAllByShowIdOrderByRowLabelAscSeatNumberAscIdAsc(UUID showId);

    long countByShowId(UUID showId);

    long countByShowIdAndAvailabilityStatus(UUID showId, ShowSeatAvailability availabilityStatus);

}
