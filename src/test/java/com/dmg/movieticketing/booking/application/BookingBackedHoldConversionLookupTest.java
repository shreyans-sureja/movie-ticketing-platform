package com.dmg.movieticketing.booking.application;

import com.dmg.movieticketing.booking.domain.BookingRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingBackedHoldConversionLookupTest {

    @Test
    void checksConversionSynchronouslyThroughBookingRepository() {
        UUID holdId = UUID.randomUUID();
        BookingRepository repository = mock(BookingRepository.class);
        when(repository.existsBySourceHoldId(holdId)).thenReturn(true);
        var lookup = new BookingBackedHoldConversionLookup(repository);

        boolean converted = lookup.isConverted(holdId);

        assertThat(converted).isTrue();
        verify(repository).existsBySourceHoldId(holdId);
    }
}
