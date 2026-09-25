package com.dmg.movieticketing.theatre.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditoriumRepository extends JpaRepository<Auditorium, UUID> {

    List<Auditorium> findAllByTheatreIdOrderByNameAscIdAsc(UUID theatreId);

    Optional<Auditorium> findByIdAndTheatreId(UUID id, UUID theatreId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT auditorium
            FROM Auditorium auditorium
            JOIN FETCH auditorium.theatre theatre
            JOIN FETCH theatre.city
            WHERE auditorium.id = :auditoriumId
              AND theatre.id = :theatreId
            """)
    Optional<Auditorium> findByIdAndTheatreIdForUpdate(
            @Param("auditoriumId") UUID auditoriumId,
            @Param("theatreId") UUID theatreId
    );

    boolean existsByTheatreIdAndNameIgnoreCase(UUID theatreId, String name);
}
