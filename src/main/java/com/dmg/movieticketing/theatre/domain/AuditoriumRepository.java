package com.dmg.movieticketing.theatre.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditoriumRepository extends JpaRepository<Auditorium, UUID> {

    List<Auditorium> findAllByTheatreIdOrderByNameAscIdAsc(UUID theatreId);

    Optional<Auditorium> findByIdAndTheatreId(UUID id, UUID theatreId);

    boolean existsByTheatreIdAndNameIgnoreCase(UUID theatreId, String name);
}
