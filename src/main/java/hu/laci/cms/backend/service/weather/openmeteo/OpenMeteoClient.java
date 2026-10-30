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
import hu.laci.cms.backend.service.weather.HourlyWeatherResult;
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
            parameters.put("forecast_hours", "24");
            parameters.put("current", "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,precipitation,rain,snowfall,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day");
            parameters.put("hourly", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,precipitation,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m");
            parameters.put("daily", "weather_code,temperature_2m_min,temperature_2m_max,apparent_temperature_min,apparent_temperature_max,precipitation_probability_max,precipitation_sum,rain_sum,snowfall_sum,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,sunrise,sunset,daylight_duration,sunshine_duration");
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
        if (vendor.current == null || vendor.hourly == null || vendor.daily == null) {
            throw invalid();
        }
        OpenMeteoForecastResponse.OpenMeteoCurrent current = vendor.current;
        Boolean isDay = mapIsDay(current.is_day);
        List<HourlyWeatherResult> hourly = mapHourly(vendor.hourly);
        List<DailyWeatherResult> daily = mapDaily(vendor.daily);
        return new WeatherResult(new CurrentWeatherResult(current.temperature_2m, current.apparent_temperature,
                current.relative_humidity_2m, current.weather_code, current.precipitation, current.rain,
                current.snowfall, current.cloud_cover, current.pressure_msl, current.wind_speed_10m,
                current.wind_direction_10m, current.wind_gusts_10m, isDay), hourly, daily);
    }

    private Boolean mapIsDay(Integer isDay) {
        if (isDay == null) {
            return null;
        }
        if (isDay != 0 && isDay != 1) {
            throw invalid();
        }
        return isDay == 1;
    }

    private List<HourlyWeatherResult> mapHourly(OpenMeteoForecastResponse.OpenMeteoHourly hourly) {
        if (hourly.time == null || hourly.temperature_2m == null || hourly.apparent_temperature == null
                || hourly.relative_humidity_2m == null || hourly.precipitation_probability == null
                || hourly.precipitation == null || hourly.weather_code == null || hourly.cloud_cover == null
                || hourly.wind_speed_10m == null || hourly.wind_direction_10m == null || hourly.wind_gusts_10m == null) {
            throw invalid();
        }
        int size = hourly.time.size();
        requireSize(size, hourly.temperature_2m, hourly.apparent_temperature, hourly.relative_humidity_2m,
                hourly.precipitation_probability, hourly.precipitation, hourly.weather_code, hourly.cloud_cover,
                hourly.wind_speed_10m, hourly.wind_direction_10m, hourly.wind_gusts_10m);
        List<HourlyWeatherResult> result = new ArrayList<>();
        for (int index = 0; index < Math.min(size, 24); index++) {
            String time = hourly.time.get(index);
            if (time == null || time.isBlank()) {
                throw invalid();
            }
            result.add(new HourlyWeatherResult(time, hourly.temperature_2m.get(index), hourly.apparent_temperature.get(index),
                    hourly.relative_humidity_2m.get(index), hourly.precipitation_probability.get(index),
                    hourly.precipitation.get(index), hourly.weather_code.get(index), hourly.cloud_cover.get(index),
                    hourly.wind_speed_10m.get(index), hourly.wind_direction_10m.get(index), hourly.wind_gusts_10m.get(index)));
        }
        return List.copyOf(result);
    }

    private List<DailyWeatherResult> mapDaily(OpenMeteoForecastResponse.OpenMeteoDaily daily) {
        if (daily.time == null || daily.weather_code == null || daily.temperature_2m_min == null || daily.temperature_2m_max == null
                || daily.apparent_temperature_min == null || daily.apparent_temperature_max == null || daily.precipitation_probability_max == null
                || daily.precipitation_sum == null || daily.rain_sum == null || daily.snowfall_sum == null || daily.wind_speed_10m_max == null
                || daily.wind_gusts_10m_max == null || daily.wind_direction_10m_dominant == null || daily.sunrise == null || daily.sunset == null
                || daily.daylight_duration == null || daily.sunshine_duration == null) {
            throw invalid();
        }
        int size = daily.time.size();
        requireSize(size, daily.weather_code, daily.temperature_2m_min, daily.temperature_2m_max, daily.apparent_temperature_min,
                daily.apparent_temperature_max, daily.precipitation_probability_max, daily.precipitation_sum, daily.rain_sum,
                daily.snowfall_sum, daily.wind_speed_10m_max, daily.wind_gusts_10m_max, daily.wind_direction_10m_dominant,
                daily.sunrise, daily.sunset, daily.daylight_duration, daily.sunshine_duration);
        List<DailyWeatherResult> days = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            String date = daily.time.get(index);
            String sunrise = daily.sunrise.get(index);
            String sunset = daily.sunset.get(index);
            if (date == null || date.isBlank() || sunrise == null || sunrise.isBlank() || sunset == null || sunset.isBlank()) {
                throw invalid();
            }
            days.add(new DailyWeatherResult(date, daily.weather_code.get(index), daily.temperature_2m_min.get(index),
                    daily.temperature_2m_max.get(index), daily.apparent_temperature_min.get(index),
                    daily.apparent_temperature_max.get(index), daily.precipitation_probability_max.get(index),
                    daily.precipitation_sum.get(index), daily.rain_sum.get(index), daily.snowfall_sum.get(index),
                    daily.wind_speed_10m_max.get(index), daily.wind_gusts_10m_max.get(index), daily.wind_direction_10m_dominant.get(index),
                    sunrise, sunset, daily.daylight_duration.get(index), daily.sunshine_duration.get(index)));
        }
        return List.copyOf(days);
    }

    private void requireSize(int expected, List<?>... arrays) {
        for (List<?> array : arrays) {
            if (array.size() != expected) {
                throw invalid();
            }
        }
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
