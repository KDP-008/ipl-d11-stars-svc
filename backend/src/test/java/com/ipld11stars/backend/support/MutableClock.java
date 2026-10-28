package com.ipld11stars.backend.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * UTC clock that tests can move. Starts at the real current time.
 */
public class MutableClock extends Clock {

    private volatile Instant instant = Instant.now();

    public void reset() {
        instant = Instant.now();
    }

    public void advance(Duration d) {
        instant = instant.plus(d);
    }

    public void setTo(LocalDateTime utc) {
        instant = utc.toInstant(ZoneOffset.UTC);
    }

    public LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
