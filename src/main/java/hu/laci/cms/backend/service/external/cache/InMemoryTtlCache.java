package hu.laci.cms.backend.service.external.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Concurrent lazy-expiration in-memory implementation suitable for servlet request concurrency. */
public final class InMemoryTtlCache<K, V> implements TtlCache<K, V> {
    private final ConcurrentHashMap<K, Entry<V>> entries = new ConcurrentHashMap<>();
    @Override public Optional<V> get(K key) { Entry<V> entry = entries.get(key); if (entry == null) return Optional.empty(); if (entry.expiresAtMillis < System.currentTimeMillis()) { entries.remove(key, entry); return Optional.empty(); } return Optional.of(entry.value); }
    @Override public void put(K key, V value, Duration ttl) { if (ttl == null || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("Cache TTL must be positive."); entries.put(key, new Entry<>(value, System.currentTimeMillis() + ttl.toMillis())); }
    @Override public void invalidate(K key) { entries.remove(key); }
    private record Entry<V>(V value, long expiresAtMillis) { }
}
