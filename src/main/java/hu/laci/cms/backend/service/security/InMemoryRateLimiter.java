package hu.laci.cms.backend.service.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory limiter for temporary lockouts.
 * <p>
 * The limiter is intentionally process-local and should be replaced with a
 * shared store if the application is scaled across multiple JVMs.
 */
public class InMemoryRateLimiter {

    private final int maxFailures;
    private final Duration lockDuration;
    private final Clock clock;
    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int maxFailures, Duration lockDuration) {
        this(maxFailures, lockDuration, Clock.systemUTC());
    }

    public InMemoryRateLimiter(int maxFailures, Duration lockDuration, Clock clock) {
        this.maxFailures = Math.max(1, maxFailures);
        this.lockDuration = Objects.requireNonNull(lockDuration, "lockDuration must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public boolean isLocked(String key) {
        AttemptState state = attempts.get(key);
        if (state == null) {
            return false;
        }

        synchronized (state) {
            if (state.lockedUntil == null) {
                return false;
            }
            if (Instant.now(clock).isBefore(state.lockedUntil)) {
                return true;
            }
            attempts.remove(key, state);
            return false;
        }
    }

    public void recordFailure(String key) {
        AttemptState state = attempts.computeIfAbsent(key, ignored -> new AttemptState());
        synchronized (state) {
            state.failureCount++;
            if (state.failureCount >= maxFailures) {
                state.lockedUntil = Instant.now(clock).plus(lockDuration);
            }
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private static final class AttemptState {
        private int failureCount;
        private Instant lockedUntil;
    }
}
