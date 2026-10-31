package hu.laci.cms.backend.service.weather;

import hu.laci.cms.backend.service.external.cache.InMemoryTtlCache;
import hu.laci.cms.backend.service.weather.openmeteo.OpenMeteoClient;

/** Production composition holder that shares the Weather Explorer provider and typed caches across functions. */
public final class WeatherServiceFactory {
    private static final WeatherService INSTANCE = new WeatherService(new OpenMeteoClient(), new InMemoryTtlCache<>(), new InMemoryTtlCache<>());
    private WeatherServiceFactory() { }
    /** @return shared production weather service */
    public static WeatherService getInstance() { return INSTANCE; }
}
