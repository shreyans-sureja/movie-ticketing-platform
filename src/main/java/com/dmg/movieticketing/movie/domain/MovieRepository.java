package com.dmg.movieticketing.movie.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    boolean existsByTitleIgnoreCaseAndLanguageCodeIgnoreCaseAndDurationMinutes(
            String title,
            String languageCode,
            int durationMinutes
    );

    @Query("""
            SELECT movie
            FROM Movie movie
            WHERE (:query IS NULL OR LOWER(movie.title) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:languageCode IS NULL OR movie.languageCode = :languageCode)
            """)
    Page<Movie> search(
            @Param("query") String query,
            @Param("languageCode") String languageCode,
            Pageable pageable
    );
}
