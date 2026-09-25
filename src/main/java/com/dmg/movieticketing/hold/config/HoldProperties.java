package com.dmg.movieticketing.hold.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "booking")
public record HoldProperties(Duration holdDuration) {

    public HoldProperties {
        if (holdDuration == null || holdDuration.isZero() || holdDuration.isNegative()) {
            throw new IllegalArgumentException("booking.hold-duration must be a positive duration.");
        }
    }
}
