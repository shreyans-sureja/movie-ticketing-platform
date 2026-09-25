package com.dmg.movieticketing.show.domain;

import com.dmg.movieticketing.show.application.ShowSearchItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MovieShowRepository extends JpaRepository<MovieShow, UUID> {

    boolean existsByAuditoriumIdAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID auditoriumId,
            Instant requestedEnd,
            Instant requestedStart
    );

    @Query(
            value = """
            SELECT new com.dmg.movieticketing.show.application.ShowSearchItem(
                movieShow.id,
                movie.id,
                movie.title,
                movie.durationMinutes,
                movie.languageCode,
                theatre.id,
                theatre.name,
                auditorium.id,
                auditorium.name,
                movieShow.startsAt,
                movieShow.endsAt,
                movieShow.currency,
                MIN(showSeat.price),
                MAX(showSeat.price),
                SUM(CASE
                    WHEN showSeat.availabilityStatus = com.dmg.movieticketing.show.domain.ShowSeatAvailability.AVAILABLE
                      OR (
                          showSeat.availabilityStatus = com.dmg.movieticketing.show.domain.ShowSeatAvailability.HELD
                          AND currentHold.expiresAt <= :requestNow
                      )
                    THEN 1 ELSE 0
                END)
            )
            FROM MovieShow movieShow
            JOIN movieShow.movie movie
            JOIN movieShow.auditorium auditorium
            JOIN auditorium.theatre theatre
            JOIN ShowSeat showSeat ON showSeat.show = movieShow
            LEFT JOIN SeatHold currentHold ON currentHold.id = showSeat.currentHoldId
            WHERE theatre.city.id = :cityId
              AND movieShow.movie.id = :movieId
              AND movieShow.startsAt >= :dayStart
              AND movieShow.startsAt < :dayEnd
              AND movieShow.startsAt > :requestNow
            GROUP BY
                movieShow.id,
                movie.id,
                movie.title,
                movie.durationMinutes,
                movie.languageCode,
                theatre.id,
                theatre.name,
                auditorium.id,
                auditorium.name,
                movieShow.startsAt,
                movieShow.endsAt,
                movieShow.currency
            ORDER BY movieShow.startsAt ASC, theatre.name ASC, auditorium.name ASC, movieShow.id ASC
            """,
            countQuery = """
            SELECT COUNT(movieShow)
            FROM MovieShow movieShow
            JOIN movieShow.auditorium auditorium
            JOIN auditorium.theatre theatre
            WHERE theatre.city.id = :cityId
              AND movieShow.movie.id = :movieId
              AND movieShow.startsAt >= :dayStart
              AND movieShow.startsAt < :dayEnd
              AND movieShow.startsAt > :requestNow
            """
    )
    Page<ShowSearchItem> searchUpcoming(
            @Param("cityId") long cityId,
            @Param("movieId") UUID movieId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd,
            @Param("requestNow") Instant requestNow,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"movie", "auditorium", "auditorium.theatre", "auditorium.theatre.city"})
    Optional<MovieShow> findById(UUID id);
}
