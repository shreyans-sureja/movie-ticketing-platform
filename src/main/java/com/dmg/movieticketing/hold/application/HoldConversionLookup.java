package com.dmg.movieticketing.hold.application;

import java.util.UUID;

public interface HoldConversionLookup {

    boolean isConverted(UUID holdId);
}
