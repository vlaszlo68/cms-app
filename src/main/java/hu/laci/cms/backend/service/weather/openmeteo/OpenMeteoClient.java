package hu.laci.cms.backend.service.weather.openmeteo;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import hu.laci.cms.backend.config.external.ExternalApiConfig;
import hu.laci.cms.backend.service.external.http.ExternalApiException;
import hu.laci.cms.backend.service.external.http.ExternalHttpExecutor;
import hu.laci.cms.backend.service.external.http.ExternalHttpRequest;
import hu.laci.cms.backend.service.external.http.ExternalHttpResponse;
import hu.laci.cms.backend.service.external.http.JavaExternalHttpExecutor;
import hu.laci.cms.backend.service.external.http.TrustedUriBuilder;
import hu.laci.cms.backend.service.weather.CurrentWeatherResult;
import hu.laci.cms.backend.service.weather.DailyWeatherResult;
import hu.laci.cms.backend.service.weather.LocationResult;
import hu.laci.cms.backend.service.weather.WeatherProviderClient;
import hu.laci.cms.backend.service.weather.WeatherResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Open-Meteo adapter that owns fixed endpoint construction and vendor-only DTO mapping. */
public final class OpenMeteoClient implements WeatherProviderClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenMeteoClient.class);
    private final ExternalHttpExecutor executor;
    private final URI geocodingBaseUrl;
    private final URI forecastBaseUrl;
    private final Duration timeout;
    private final Gson gson;

    /** Creates a production client from trusted backend configuration. */
    public OpenMeteoClient() {
        this(new JavaExternalHttpExecutor(Duration.ofSeconds(ExternalApiConfig.getCurrent().getConnectTimeoutSeconds())), ExternalApiConfig.getCurrent(), new Gson());
    }

    /** Creates a client from application configuration. */
    public OpenMeteoClient(ExternalHttpExecutor executor, ExternalApiConfig config, Gson gson) {
        this(executor, config.getOpenMeteoGeocodingBaseUrl(), config.getOpenMeteoForecastBaseUrl(), Duration.ofSeconds(config.getRequestTimeoutSeconds()), gson);
    }

    /** Creates an explicitly configured client for isolated provider tests. */
    public OpenMeteoClient(ExternalHttpExecutor executor, URI geocodingBaseUrl, URI forecastBaseUrl, Duration timeout, Gson gson) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.geocodingBaseUrl = Objects.requireNonNull(geocodingBaseUrl, "geocodingBaseUrl");
        this.forecastBaseUrl = Objects.requireNonNull(forecastBaseUrl, "forecastBaseUrl");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    /** Searches Open-Meteo's fixed geocoding endpoint and maps vendor DTOs to domain results. */
    @Override
    public List<LocationResult> searchLocations(String query, String language) {
        long started = System.nanoTime();
        try {
            URI uri = TrustedUriBuilder.build(geocodingBaseUrl, "/v1/search", Map.of("name", query, "count", "10", "language", language, "format", "json"));
            ExternalHttpResponse response = executor.execute(ExternalHttpRequest.get(uri, timeout));
            List<LocationResult> result = mapLocations(parse(response.body(), OpenMeteoGeocodingResponse.class));
            log("geocoding", response.statusCode(), started, true);
            return result;
        } catch (ExternalApiException exception) {
            log("geocoding", status(exception), started, false);
            throw exception;
        } catch (RuntimeException exception) {
            log("geocoding", 0, started, false);
            throw invalid();
        }
    }

    /** Retrieves Open-Meteo's fixed forecast endpoint and maps vendor DTOs to domain results. */
    @Override
    public WeatherResult getWeather(double latitude, double longitude, String timezone) {
        long started = System.nanoTime();
        try {
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("latitude", String.valueOf(latitude));
            parameters.put("longitude", String.valueOf(longitude));
            parameters.put("timezone", timezone);
            parameters.put("forecast_days", "7");
            parameters.put("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m");
            parameters.put("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max");
            ExternalHttpResponse response = executor.execute(ExternalHttpRequest.get(TrustedUriBuilder.build(forecastBaseUrl, "/v1/forecast", parameters), timeout));
            WeatherResult result = mapForecast(parse(response.body(), OpenMeteoForecastResponse.class));
            log("forecast", response.statusCode(), started, true);
            return result;
        } catch (ExternalApiException exception) {
            log("forecast", status(exception), started, false);
            throw exception;
        } catch (RuntimeException exception) {
            log("forecast", 0, started, false);
            throw invalid();
        }
    }

    private <T> T parse(String body, Class<T> type) {
        try {
            T parsed = gson.fromJson(body, type);
            if (parsed == null) {
                throw invalid();
            }
            return parsed;
        } catch (JsonParseException | IllegalStateException exception) {
            throw invalid();
        }
    }

    private List<LocationResult> mapLocations(OpenMeteoGeocodingResponse vendor) {
        if (vendor.results == null) {
            return List.of();
        }
        List<LocationResult> results = new ArrayList<>();
        for (OpenMeteoGeocodingResponse.OpenMeteoLocation location : vendor.results) {
            if (location == null || location.name == null || location.name.isBlank() || location.latitude == null || location.longitude == null || location.timezone == null || location.timezone.isBlank()) {
                throw invalid();
            }
            results.add(new LocationResult(location.id, location.name, location.country, location.country_code, location.admin1, location.latitude, location.longitude, location.timezone));
        }
        return List.copyOf(results);
    }

    private WeatherResult mapForecast(OpenMeteoForecastResponse vendor) {
        if (vendor.current == null || vendor.daily == null) {
            throw invalid();
        }
        OpenMeteoForecastResponse.OpenMeteoCurrent current = vendor.current;
        if (current.temperature_2m == null || current.apparent_temperature == null || current.relative_humidity_2m == null || current.wind_speed_10m == null || current.weather_code == null) {
            throw invalid();
        }
        OpenMeteoForecastResponse.OpenMeteoDaily daily = vendor.daily;
        if (daily.time == null || daily.weather_code == null || daily.temperature_2m_min == null || daily.temperature_2m_max == null || daily.precipitation_probability_max == null) {
            throw invalid();
        }
        int size = daily.time.size();
        if (daily.weather_code.size() != size || daily.temperature_2m_min.size() != size || daily.temperature_2m_max.size() != size || daily.precipitation_probability_max.size() != size) {
            throw invalid();
        }
        List<DailyWeatherResult> days = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            String date = daily.time.get(index);
            Integer weatherCode = daily.weather_code.get(index);
            Double minimum = daily.temperature_2m_min.get(index);
            Double maximum = daily.temperature_2m_max.get(index);
            Integer probability = daily.precipitation_probability_max.get(index);
            if (date == null || date.isBlank() || weatherCode == null || minimum == null || maximum == null || probability == null) {
                throw invalid();
            }
            days.add(new DailyWeatherResult(date, weatherCode, minimum, maximum, probability));
        }
        return new WeatherResult(new CurrentWeatherResult(current.temperature_2m, current.apparent_temperature, current.relative_humidity_2m, current.wind_speed_10m, current.weather_code), days);
    }

    private ExternalApiException invalid() {
        return new ExternalApiException(ExternalApiException.INVALID_RESPONSE, "External service returned an invalid response.");
    }

    private int status(ExternalApiException exception) {
        return exception.getHttpStatus() == null ? 0 : exception.getHttpStatus();
    }

    private void log(String operation, int status, long started, boolean success) {
        LOGGER.info("provider=open-meteo operation={} status={} durationMs={} success={}", operation, status, (System.nanoTime() - started) / 1_000_000L, success);
    }
}
