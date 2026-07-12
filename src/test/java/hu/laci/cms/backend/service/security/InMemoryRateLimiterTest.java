package hu.laci.cms.backend.service.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Unit tests for in-memory failed-attempt lockout behavior. */
class InMemoryRateLimiterTest {

    @Test
    void locksAfterConfiguredFailuresExpiresAndClearsOnSuccess() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-12T10:00:00Z"));
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(2, Duration.ofMinutes(1), clock);

        limiter.recordFailure("client");
        Assertions.assertFalse(limiter.isLocked("client"));
        limiter.recordFailure("client");
        Assertions.assertTrue(limiter.isLocked("client"));

        clock.advance(Duration.ofMinutes(1));
        Assertions.assertFalse(limiter.isLocked("client"));
        limiter.recordFailure("client");
        limiter.recordSuccess("client");
        Assertions.assertFalse(limiter.isLocked("client"));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
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
}
