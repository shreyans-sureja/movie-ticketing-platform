package com.dmg.movieticketing.hold.application;

import java.util.UUID;

/**
 * Hold-facing port for determining whether a hold has already become a booking.
 * Keeping the port here avoids a hold-to-booking repository dependency.
 */
public interface HoldConversionLookup {

    /** Returns whether the hold is the source of a persisted booking. */
    boolean isConverted(UUID holdId);
}
