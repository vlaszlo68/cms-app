package hu.laci.cms.backend.service.weather;

import hu.laci.cms.backend.dto.weather.*;
import hu.laci.cms.backend.service.external.cache.TtlCache;
import hu.laci.cms.backend.service.function.PublicFunctionException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Weather application service for validation, provider-neutral mapping, and typed TTL caching. */
public final class WeatherService {
    private static final Duration LOCATION_TTL = Duration.ofMinutes(30);
    private static final Duration WEATHER_TTL = Duration.ofMinutes(5);
    private static final Pattern LANGUAGE = Pattern.compile("^[A-Za-z]{2,10}(-[A-Za-z]{2,10})?$");
    private final WeatherProviderClient provider;
    private final TtlCache<String, List<LocationResult>> locationCache;
    private final TtlCache<String, WeatherResult> weatherCache;
    public WeatherService(WeatherProviderClient provider, TtlCache<String, List<LocationResult>> locationCache, TtlCache<String, WeatherResult> weatherCache) { this.provider = Objects.requireNonNull(provider); this.locationCache = Objects.requireNonNull(locationCache); this.weatherCache = Objects.requireNonNull(weatherCache); }
    /** Searches matching places using the normalized query/language cache identity. */
    public SearchLocationResponse searchLocations(SearchLocationRequest request) {
        if (request == null || request.getQuery() == null) throw invalid("Location query is required.");
        String query = request.getQuery().trim(); if (query.length() < 2 || query.length() > 100) throw invalid("Location query must contain 2 to 100 characters.");
        String language = request.getLanguage() == null || request.getLanguage().isBlank() ? "en" : request.getLanguage().trim(); if (!LANGUAGE.matcher(language).matches()) throw invalid("Language must be a short language code.");
        String key = query.toLowerCase(Locale.ROOT) + "|" + language.toLowerCase(Locale.ROOT);
        List<LocationResult> results = locationCache.get(key).orElseGet(() -> { List<LocationResult> value = provider.searchLocations(query, language); locationCache.put(key, value, LOCATION_TTL); return value; });
        return new SearchLocationResponse(results.stream().map(item -> new LocationDto(item.id(), item.name(), item.country(), item.countryCode(), item.admin1(), item.latitude(), item.longitude(), item.timezone())).toList());
    }
    /** Returns current, next-24-hour, and daily weather at validated coordinates. Coordinates are rounded to three decimals for cache identity. */
    public GetWeatherResponse getWeather(GetWeatherRequest request) {
        if (request == null || request.getLatitude() == null || request.getLongitude() == null || !Double.isFinite(request.getLatitude()) || !Double.isFinite(request.getLongitude())) throw invalid("Latitude and longitude are required.");
        double latitude = request.getLatitude(); double longitude = request.getLongitude(); if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) throw invalid("Coordinates are outside the supported range.");
        String timezone = request.getTimezone() == null || request.getTimezone().isBlank() ? "auto" : request.getTimezone().trim(); if (timezone.length() > 100) throw invalid("Timezone is too long.");
        double roundedLatitude = round(latitude); double roundedLongitude = round(longitude); String key = roundedLatitude + "|" + roundedLongitude + "|" + timezone.toLowerCase(Locale.ROOT);
        WeatherResult result = weatherCache.get(key).orElseGet(() -> { WeatherResult value = provider.getWeather(latitude, longitude, timezone); weatherCache.put(key, value, WEATHER_TTL); return value; });
        CurrentWeatherResult current = result.current();
        CurrentWeatherDto currentDto = new CurrentWeatherDto(current.temperature(), current.apparentTemperature(),
                current.relativeHumidity(), current.weatherCode(), current.precipitation(), current.rain(), current.snowfall(),
                current.cloudCover(), current.pressureMsl(), current.windSpeed(), current.windDirection(), current.windGusts(), current.isDay());
        List<HourlyWeatherDto> hourly = result.hourly().stream().map(item -> new HourlyWeatherDto(item.time(), item.temperature(),
                item.apparentTemperature(), item.relativeHumidity(), item.precipitationProbability(), item.precipitation(),
                item.weatherCode(), item.cloudCover(), item.windSpeed(), item.windDirection(), item.windGusts())).toList();
        List<DailyWeatherDto> daily = result.daily().stream().map(item -> new DailyWeatherDto(item.date(), item.weatherCode(),
                item.temperatureMin(), item.temperatureMax(), item.apparentTemperatureMin(), item.apparentTemperatureMax(),
                item.precipitationProbability(), item.precipitationSum(), item.rainSum(), item.snowfallSum(), item.windSpeedMax(),
                item.windGustsMax(), item.windDirectionDominant(), item.sunrise(), item.sunset(), item.daylightDurationSeconds(),
                item.sunshineDurationSeconds())).toList();
        return new GetWeatherResponse(currentDto, hourly, daily);
    }
    private double round(double value) { return Math.round(value * 1000.0d) / 1000.0d; }
    private PublicFunctionException invalid(String message) { return new PublicFunctionException("INVALID_FUNCTION_INPUT", message); }
}
