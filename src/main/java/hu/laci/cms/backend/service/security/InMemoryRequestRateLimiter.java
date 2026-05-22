package hu.laci.cms.backend.service.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory fixed-window request rate limiter.
 * <p>
 * This limiter is intended for short-lived request throttling, such as limiting
 * how many CAPTCHA images a client can generate in a brief time window.
 */
public class InMemoryRequestRateLimiter {

    private final int maxRequests;
    private final Duration windowDuration;
    private final Clock clock;
    private final ConcurrentMap<String, WindowState> windows = new ConcurrentHashMap<>();

    /**
     * Creates a fixed-window request limiter using the system UTC clock.
     *
     * @param maxRequests maximum allowed requests in one window
     * @param windowDuration window duration
     */
    public InMemoryRequestRateLimiter(int maxRequests, Duration windowDuration) {
        this(maxRequests, windowDuration, Clock.systemUTC());
    }

    /**
     * Creates a fixed-window request limiter using a caller-provided clock.
     *
     * @param maxRequests maximum allowed requests in one window
     * @param windowDuration window duration
     * @param clock clock used to evaluate windows
     */
    public InMemoryRequestRateLimiter(int maxRequests, Duration windowDuration, Clock clock) {
        this.maxRequests = Math.max(1, maxRequests);
        this.windowDuration = Objects.requireNonNull(windowDuration, "windowDuration must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Records a request attempt and returns whether it is allowed.
     *
     * @param key client-specific limiter key
     * @return true when the request is within the configured window limit
     */
    public boolean allowRequest(String key) {
        Instant now = Instant.now(clock);
        WindowState state = windows.computeIfAbsent(key, ignored -> new WindowState(now, 0));
        synchronized (state) {
            if (!now.isBefore(state.windowStartedAt.plus(windowDuration))) {
                state.windowStartedAt = now;
                state.requestCount = 0;
            }
            state.requestCount++;
            return state.requestCount <= maxRequests;
        }
    }

    private static final class WindowState {
        private Instant windowStartedAt;
        private int requestCount;

        private WindowState(Instant windowStartedAt, int requestCount) {
            this.windowStartedAt = windowStartedAt;
            this.requestCount = requestCount;
        }
    }
}
