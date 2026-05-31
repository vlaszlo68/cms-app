CREATE TABLE rate_limits (
    limiter_name VARCHAR(100) NOT NULL,
    limiter_key VARCHAR(255) NOT NULL,
    failure_count INTEGER NOT NULL DEFAULT 0,
    request_count INTEGER NOT NULL DEFAULT 0,
    window_started_at TIMESTAMP NULL,
    locked_until TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (limiter_name, limiter_key)
);

CREATE INDEX idx_rate_limits_locked_until ON rate_limits (locked_until);
CREATE INDEX idx_rate_limits_updated_at ON rate_limits (updated_at);
