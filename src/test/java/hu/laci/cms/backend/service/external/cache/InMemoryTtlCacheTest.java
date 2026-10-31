package hu.laci.cms.backend.service.external.cache;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryTtlCacheTest {
    @Test void returnsReplacementAndExpiresEntries() throws Exception {
        TtlCache<String, String> cache = new InMemoryTtlCache<>();
        cache.put("key", "first", Duration.ofMillis(100));
        assertEquals("first", cache.get("key").orElseThrow());
        cache.put("key", "replacement", Duration.ofMillis(10));
        assertEquals("replacement", cache.get("key").orElseThrow());
        Thread.sleep(30);
        assertTrue(cache.get("key").isEmpty());
    }

    @Test void toleratesConcurrentGetAndPut() throws Exception {
        TtlCache<Integer, Integer> cache = new InMemoryTtlCache<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch complete = new CountDownLatch(4);
        for (int worker = 0; worker < 4; worker++) {
            final int value = worker;
            executor.execute(() -> { for (int index = 0; index < 100; index++) { cache.put(index, value, Duration.ofSeconds(1)); cache.get(index); } complete.countDown(); });
        }
        complete.await();
        executor.shutdownNow();
        assertTrue(cache.get(50).isPresent());
    }
}
