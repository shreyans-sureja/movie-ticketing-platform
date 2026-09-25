package com.dmg.movieticketing.show.application;

import com.dmg.movieticketing.city.domain.CityRepository;
import com.dmg.movieticketing.movie.domain.Movie;
import com.dmg.movieticketing.movie.domain.MovieRepository;
import com.dmg.movieticketing.show.domain.MovieShowRepository;
import com.dmg.movieticketing.show.domain.ShowSeatRepository;
import com.dmg.movieticketing.show.domain.ShowTierPriceRepository;
import com.dmg.movieticketing.theatre.application.DomainValidationException;
import com.dmg.movieticketing.theatre.domain.Auditorium;
import com.dmg.movieticketing.theatre.domain.AuditoriumRepository;
import com.dmg.movieticketing.theatre.domain.PhysicalSeatRepository;
import com.dmg.movieticketing.theatre.domain.Theatre;
import com.dmg.movieticketing.theatre.domain.TheatreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private CityRepository cityRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private TheatreRepository theatreRepository;
    @Mock
    private AuditoriumRepository auditoriumRepository;
    @Mock
    private PhysicalSeatRepository physicalSeatRepository;
    @Mock
    private MovieShowRepository movieShowRepository;
    @Mock
    private ShowTierPriceRepository showTierPriceRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;

    private ShowService showService;

    @BeforeEach
    void setUp() {
        showService = new ShowService(
                cityRepository,
                movieRepository,
                theatreRepository,
                auditoriumRepository,
                physicalSeatRepository,
                movieShowRepository,
                showTierPriceRepository,
                showSeatRepository,
                CLOCK
        );
    }

    @Test
    void currentKolkataDateUsesUtcBoundsAndExcludesStartedShows() {
        UUID movieId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        LocalDate customerDate = LocalDate.of(2026, 9, 25);
        Instant dayStart = Instant.parse("2026-09-24T18:30:00Z");
        Instant dayEnd = Instant.parse("2026-09-25T18:30:00Z");
        ShowSearchItem aggregateResult = new ShowSearchItem(
                UUID.randomUUID(),
                movieId,
                "The Last Signal",
                128,
                "hi",
                UUID.randomUUID(),
                "Central Cinema",
                UUID.randomUUID(),
                "Screen 1",
                Instant.parse("2026-09-25T12:00:00Z"),
                Instant.parse("2026-09-25T14:08:00Z"),
                "INR",
                new BigDecimal("250.00"),
                new BigDecimal("400.00"),
                20L
        );

        when(cityRepository.existsById(7L)).thenReturn(true);
        when(movieRepository.existsById(movieId)).thenReturn(true);
        when(movieShowRepository.searchUpcoming(7L, movieId, dayStart, dayEnd, NOW, pageable))
                .thenReturn(new PageImpl<>(List.of(aggregateResult), pageable, 1));

        Page<ShowSearchItem> result = showService.searchShows(7L, movieId, customerDate, pageable);

        assertThat(result.getContent()).containsExactly(aggregateResult);
        verify(movieShowRepository).searchUpcoming(7L, movieId, dayStart, dayEnd, NOW, pageable);
        verifyNoInteractions(showSeatRepository, showTierPriceRepository);
    }

    @Test
    void pastCustomerDateReturnsEmptyWithoutQueryingShows() {
        UUID movieId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        when(cityRepository.existsById(7L)).thenReturn(true);
        when(movieRepository.existsById(movieId)).thenReturn(true);

        showService.searchShows(7L, movieId, LocalDate.of(2026, 9, 24), pageable);

        verify(movieShowRepository, never()).searchUpcoming(
                7L,
                movieId,
                Instant.parse("2026-09-23T18:30:00Z"),
                Instant.parse("2026-09-24T18:30:00Z"),
                NOW,
                pageable
        );
    }

    @Test
    void showStartMustBeAfterInjectedClockInstant() {
        UUID accountId = UUID.randomUUID();
        UUID theatreId = UUID.randomUUID();
        UUID auditoriumId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        Theatre theatre = org.mockito.Mockito.mock(Theatre.class);
        Auditorium auditorium = org.mockito.Mockito.mock(Auditorium.class);
        Movie movie = org.mockito.Mockito.mock(Movie.class);

        when(theatreRepository.findByIdAndOwnerAccountId(theatreId, accountId))
                .thenReturn(Optional.of(theatre));
        when(auditoriumRepository.findByIdAndTheatreIdForUpdate(auditoriumId, theatreId))
                .thenReturn(Optional.of(auditorium));
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));

        assertThatThrownBy(() -> showService.createShow(
                accountId,
                theatreId,
                auditoriumId,
                movieId,
                NOW,
                "INR",
                List.of()
        )).isInstanceOf(DomainValidationException.class)
                .hasMessage("Show start must be in the future.");

        verify(movieShowRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
