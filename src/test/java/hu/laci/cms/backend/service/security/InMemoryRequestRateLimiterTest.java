package hu.laci.cms.backend.service.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRequestRateLimiterTest {

    @Test
    void allowRequestResetsCountAfterWindowExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-22T10:00:00Z"));
        InMemoryRequestRateLimiter limiter = new InMemoryRequestRateLimiter(2, Duration.ofSeconds(10), clock);

        assertTrue(limiter.allowRequest("client-1"));
        assertTrue(limiter.allowRequest("client-1"));
        assertFalse(limiter.allowRequest("client-1"));

        clock.advance(Duration.ofSeconds(10));

        assertTrue(limiter.allowRequest("client-1"));
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
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
