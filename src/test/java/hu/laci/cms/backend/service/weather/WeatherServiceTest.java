package hu.laci.cms.backend.service.weather;

import hu.laci.cms.backend.dto.weather.*;
import hu.laci.cms.backend.service.external.cache.InMemoryTtlCache;
import hu.laci.cms.backend.service.function.PublicFunctionException;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {
    @Test void cachesNormalizedLocationSearchesAndWeatherAndValidatesCoordinates() {
        AtomicInteger searches = new AtomicInteger();
        AtomicInteger forecasts = new AtomicInteger();
        WeatherProviderClient client = new WeatherProviderClient() {
            @Override public List<LocationResult> searchLocations(String query, String language) { searches.incrementAndGet(); return List.of(new LocationResult(1L, "Budapest", "Hungary", "HU", null, 47.5, 19.0, "Europe/Budapest")); }
            @Override public WeatherResult getWeather(double latitude, double longitude, String timezone) { forecasts.incrementAndGet(); return new WeatherResult(new CurrentWeatherResult(1, 1, 1, 1, 1), List.of()); }
        };
        WeatherService service = new WeatherService(client, new InMemoryTtlCache<>(), new InMemoryTtlCache<>());
        SearchLocationRequest first = new SearchLocationRequest(); first.setQuery(" Budapest "); first.setLanguage("EN");
        SearchLocationRequest second = new SearchLocationRequest(); second.setQuery("budapest"); second.setLanguage("en");
        assertEquals(1, service.searchLocations(first).locations().size()); service.searchLocations(second); assertEquals(1, searches.get());
        GetWeatherRequest weather = new GetWeatherRequest(); weather.setLatitude(47.4981d); weather.setLongitude(19.0398d); weather.setTimezone("Europe/Budapest");
        GetWeatherRequest equivalent = new GetWeatherRequest(); equivalent.setLatitude(47.4982d); equivalent.setLongitude(19.0399d); equivalent.setTimezone("europe/budapest");
        service.getWeather(weather); service.getWeather(equivalent); assertEquals(1, forecasts.get());
        GetWeatherRequest invalid = new GetWeatherRequest(); invalid.setLatitude(91d); invalid.setLongitude(19d);
        assertEquals("INVALID_FUNCTION_INPUT", assertThrows(PublicFunctionException.class, () -> service.getWeather(invalid)).getCode());
    }
}
