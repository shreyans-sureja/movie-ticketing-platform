package com.dmg.movieticketing.hold.application;

import com.dmg.movieticketing.hold.config.HoldProperties;
import com.dmg.movieticketing.hold.domain.SeatHold;
import com.dmg.movieticketing.hold.domain.SeatHoldItemRepository;
import com.dmg.movieticketing.hold.domain.SeatHoldRepository;
import com.dmg.movieticketing.show.domain.MovieShow;
import com.dmg.movieticketing.show.domain.MovieShowRepository;
import com.dmg.movieticketing.show.domain.ShowSeat;
import com.dmg.movieticketing.show.domain.ShowSeatAvailability;
import com.dmg.movieticketing.show.domain.ShowSeatRepository;
import com.dmg.movieticketing.theatre.application.DomainValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private MovieShowRepository movieShowRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private SeatHoldItemRepository seatHoldItemRepository;
    @Mock
    private HoldConversionLookup holdConversionLookup;

    private SeatHoldService service;

    @BeforeEach
    void setUp() {
        service = new SeatHoldService(
                movieShowRepository,
                showSeatRepository,
                seatHoldRepository,
                seatHoldItemRepository,
                new HoldProperties(Duration.ofMinutes(5)),
                CLOCK,
                holdConversionLookup
        );
    }

    @Test
    void holdsEveryRequestedAvailableSeatInOneHold() {
        UUID customerId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();
        UUID firstSeatId = UUID.randomUUID();
        UUID secondSeatId = UUID.randomUUID();
        MovieShow show = org.mockito.Mockito.mock(MovieShow.class);
        ShowSeat firstSeat = org.mockito.Mockito.mock(ShowSeat.class);
        ShowSeat secondSeat = org.mockito.Mockito.mock(ShowSeat.class);

        when(show.getStartsAt()).thenReturn(NOW.plusSeconds(3600));
        when(firstSeat.getId()).thenReturn(firstSeatId);
        when(firstSeat.getRowLabel()).thenReturn("A");
        when(firstSeat.getSeatNumber()).thenReturn(1);
        when(firstSeat.getAvailabilityStatus()).thenReturn(ShowSeatAvailability.AVAILABLE);
        when(secondSeat.getId()).thenReturn(secondSeatId);
        when(secondSeat.getRowLabel()).thenReturn("A");
        when(secondSeat.getSeatNumber()).thenReturn(2);
        when(secondSeat.getAvailabilityStatus()).thenReturn(ShowSeatAvailability.AVAILABLE);
        when(movieShowRepository.findById(showId)).thenReturn(Optional.of(show));
        when(showSeatRepository.findAllForHoldUpdate(
                showId,
                List.of(firstSeatId, secondSeatId).stream().sorted().toList()
        )).thenReturn(List.of(firstSeat, secondSeat));
        when(seatHoldRepository.findAllById(any())).thenReturn(List.of());

        HoldDetails result = service.createHold(
                customerId,
                showId,
                List.of(secondSeatId, firstSeatId)
        );

        assertThat(result.status()).isEqualTo(HoldStatus.ACTIVE);
        assertThat(result.hold().getCustomerAccountId()).isEqualTo(customerId);
        assertThat(result.hold().getCreatedAt()).isEqualTo(NOW);
        assertThat(result.hold().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(result.seats()).containsExactly(firstSeat, secondSeat);
        verify(firstSeat).assignHold(result.hold().getId());
        verify(secondSeat).assignHold(result.hold().getId());
        verify(showSeatRepository).saveAllAndFlush(List.of(firstSeat, secondSeat));
    }

    @Test
    void oneActivelyHeldSeatRejectsWholeRequestWithoutWrites() {
        UUID showId = UUID.randomUUID();
        UUID availableSeatId = UUID.randomUUID();
        UUID heldSeatId = UUID.randomUUID();
        UUID activeHoldId = UUID.randomUUID();
        MovieShow show = org.mockito.Mockito.mock(MovieShow.class);
        ShowSeat availableSeat = org.mockito.Mockito.mock(ShowSeat.class);
        ShowSeat heldSeat = org.mockito.Mockito.mock(ShowSeat.class);
        SeatHold activeHold = org.mockito.Mockito.mock(SeatHold.class);

        when(show.getStartsAt()).thenReturn(NOW.plusSeconds(3600));
        when(availableSeat.getAvailabilityStatus()).thenReturn(ShowSeatAvailability.AVAILABLE);
        when(heldSeat.getAvailabilityStatus()).thenReturn(ShowSeatAvailability.HELD);
        when(heldSeat.getCurrentHoldId()).thenReturn(activeHoldId);
        when(movieShowRepository.findById(showId)).thenReturn(Optional.of(show));
        when(showSeatRepository.findAllForHoldUpdate(showId, List.of(availableSeatId, heldSeatId).stream()
                .sorted().toList())).thenReturn(List.of(availableSeat, heldSeat));
        when(seatHoldRepository.findAllById(any())).thenReturn(List.of(activeHold));
        when(activeHold.getId()).thenReturn(activeHoldId);
        when(activeHold.getExpiresAt()).thenReturn(NOW.plusSeconds(60));

        assertThatThrownBy(() -> service.createHold(
                UUID.randomUUID(),
                showId,
                List.of(availableSeatId, heldSeatId)
        )).isInstanceOf(SeatsUnavailableException.class);

        verify(seatHoldRepository, never()).save(any(SeatHold.class));
        verify(availableSeat, never()).assignHold(any());
        verify(heldSeat, never()).assignHold(any());
        verifyNoInteractions(seatHoldItemRepository);
    }

    @Test
    void expiredHoldCanBeReplacedWithoutCleanup() {
        UUID showId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID expiredHoldId = UUID.randomUUID();
        MovieShow show = org.mockito.Mockito.mock(MovieShow.class);
        ShowSeat seat = org.mockito.Mockito.mock(ShowSeat.class);
        SeatHold expiredHold = org.mockito.Mockito.mock(SeatHold.class);

        when(show.getStartsAt()).thenReturn(NOW.plusSeconds(3600));
        when(seat.getId()).thenReturn(seatId);
        when(seat.getAvailabilityStatus()).thenReturn(ShowSeatAvailability.HELD);
        when(seat.getCurrentHoldId()).thenReturn(expiredHoldId);
        when(movieShowRepository.findById(showId)).thenReturn(Optional.of(show));
        when(showSeatRepository.findAllForHoldUpdate(showId, List.of(seatId))).thenReturn(List.of(seat));
        when(seatHoldRepository.findAllById(any())).thenReturn(List.of(expiredHold));
        when(expiredHold.getId()).thenReturn(expiredHoldId);
        when(expiredHold.getExpiresAt()).thenReturn(NOW);

        HoldDetails result = service.createHold(UUID.randomUUID(), showId, List.of(seatId));

        assertThat(result.status()).isEqualTo(HoldStatus.ACTIVE);
        verify(seat).assignHold(result.hold().getId());
    }

    @Test
    void convertedStatusUsesHoldOwnedLookupBoundary() {
        UUID customerId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        SeatHold hold = org.mockito.Mockito.mock(SeatHold.class);

        when(seatHoldRepository.findByIdAndCustomerAccountId(holdId, customerId))
                .thenReturn(Optional.of(hold));
        when(seatHoldItemRepository.findAllForHold(holdId)).thenReturn(List.of());
        when(holdConversionLookup.isConverted(holdId)).thenReturn(true);

        HoldDetails result = service.getHold(customerId, holdId);

        assertThat(result.status()).isEqualTo(HoldStatus.CONVERTED);
        verify(holdConversionLookup).isConverted(holdId);
    }

    @Test
    void duplicateSeatIdsAreRejectedBeforeDatabaseAccess() {
        UUID seatId = UUID.randomUUID();

        assertThatThrownBy(() -> service.createHold(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(seatId, seatId)
        )).isInstanceOf(DomainValidationException.class)
                .hasMessage("Show seat IDs must not contain duplicates.");

        verifyNoInteractions(
                movieShowRepository,
                showSeatRepository,
                seatHoldRepository,
                seatHoldItemRepository,
                holdConversionLookup
        );
    }
}
