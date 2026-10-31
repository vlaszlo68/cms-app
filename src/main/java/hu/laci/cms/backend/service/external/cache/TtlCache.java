package hu.laci.cms.backend.service.external.cache;

import java.time.Duration;
import java.util.Optional;

/** Thread-safe cache boundary for short-lived typed provider results. */
public interface TtlCache<K, V> {
    Optional<V> get(K key);
    void put(K key, V value, Duration ttl);
    void invalidate(K key);
}
