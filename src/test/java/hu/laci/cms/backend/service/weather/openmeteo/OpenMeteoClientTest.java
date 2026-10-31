package hu.laci.cms.backend.service.weather.openmeteo;

import com.google.gson.Gson;
import hu.laci.cms.backend.service.external.http.ExternalApiException;
import hu.laci.cms.backend.service.external.http.ExternalHttpExecutor;
import hu.laci.cms.backend.service.external.http.ExternalHttpResponse;
import hu.laci.cms.backend.service.weather.WeatherResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenMeteoClientTest {
    @Test
    void mapsForecastAndGeocodingAndHandlesNoResults() {
        OpenMeteoClient forecast = client(fullForecast(25));
        WeatherResult result = forecast.getWeather(1, 2, "Europe/Budapest");
        assertEquals(24, result.hourly().size());
        assertEquals(2, result.daily().size());
        assertEquals(1d, result.current().temperature());
        assertEquals(2d, result.current().apparentTemperature());
        assertEquals(3, result.current().relativeHumidity());
        assertEquals(4, result.current().weatherCode());
        assertEquals(5d, result.current().precipitation());
        assertEquals(6d, result.current().rain());
        assertEquals(7d, result.current().snowfall());
        assertEquals(8, result.current().cloudCover());
        assertEquals(9d, result.current().pressureMsl());
        assertEquals(10d, result.current().windSpeed());
        assertEquals(11, result.current().windDirection());
        assertEquals(12d, result.current().windGusts());
        assertEquals(true, result.current().isDay());
        assertEquals("2026-01-01T01:00", result.hourly().get(1).time());
        assertEquals(1d, result.hourly().get(1).temperature());
        assertEquals(1d, result.hourly().get(1).apparentTemperature());
        assertEquals(1, result.hourly().get(1).relativeHumidity());
        assertEquals(6, result.hourly().get(1).precipitationProbability());
        assertEquals(1d, result.hourly().get(1).precipitation());
        assertEquals(1, result.hourly().get(1).weatherCode());
        assertEquals(1, result.hourly().get(1).cloudCover());
        assertEquals(1d, result.hourly().get(1).windSpeed());
        assertEquals(1, result.hourly().get(1).windDirection());
        assertEquals(1d, result.hourly().get(1).windGusts());
        assertEquals(1d, result.daily().getFirst().apparentTemperatureMin());
        assertEquals(3d, result.daily().getFirst().apparentTemperatureMax());
        assertEquals(7d, result.daily().getFirst().precipitationSum());
        assertEquals(7d, result.daily().getFirst().rainSum());
        assertEquals(0d, result.daily().getFirst().snowfallSum());
        assertEquals(9d, result.daily().getFirst().windSpeedMax());
        assertEquals(11d, result.daily().getFirst().windGustsMax());
        assertEquals(13, result.daily().getFirst().windDirectionDominant());
        assertEquals("2026-01-01T07:00", result.daily().getFirst().sunrise());
        assertEquals("2026-01-01T16:00", result.daily().getFirst().sunset());
        assertEquals(36000d, result.daily().getFirst().daylightDurationSeconds());
        assertEquals(18000d, result.daily().getFirst().sunshineDurationSeconds());
        OpenMeteoClient geocoding = client("{\"results\":[{\"id\":1,\"name\":\"Budapest\",\"latitude\":47.5,\"longitude\":19.0,\"timezone\":\"Europe/Budapest\"}]}");
        assertEquals("Budapest", geocoding.searchLocations("Budapest", "en").getFirst().name());
        assertEquals(0, client("{}").searchLocations("None", "en").size());
    }

    @Test
    void translatesMalformedVendorResponsesToControlledError() {
        assertInvalid(fullForecast(1).replace("\"weather_code\":[1]", "\"weather_code\":[{}]"));
        assertEquals(ExternalApiException.INVALID_RESPONSE, assertThrows(ExternalApiException.class, () -> client("{\"results\":[{\"name\":\"Budapest\"}]}").searchLocations("Budapest", "en")).getCode());
        assertInvalid("not-json");
    }

    @Test
    void rejectsMismatchedDailyArraysAndPropagatesProviderFailures() {
        assertInvalid(fullForecast(1).replace("\"temperature_2m\":[1]", "\"temperature_2m\":[]"));
        assertInvalid(fullForecast(1).replace("\"precipitation_sum\":[7,8]", "\"precipitation_sum\":[]"));
        ExternalHttpExecutor rateLimited = request -> { throw new ExternalApiException(ExternalApiException.RATE_LIMITED, "hidden", 429); };
        assertEquals(ExternalApiException.RATE_LIMITED, assertThrows(ExternalApiException.class, () -> new OpenMeteoClient(rateLimited, URI.create("http://stub"), URI.create("http://stub"), Duration.ofSeconds(1), new Gson()).getWeather(1, 2, "auto")).getCode());
    }

    @Test
    void requestsExactExtendedForecastParameters() {
        final java.util.concurrent.atomic.AtomicReference<URI> capturedUri = new java.util.concurrent.atomic.AtomicReference<>();
        ExternalHttpExecutor executor = request -> {
            capturedUri.set(request.uri());
            return new ExternalHttpResponse(200, Map.of(), fullForecast(1));
        };
        new OpenMeteoClient(executor, URI.create("http://stub"), URI.create("http://stub"), Duration.ofSeconds(1), new Gson())
                .getWeather(47.498, 19.0399, "Europe/Budapest");
        Map<String, String> parameters = queryParameters(capturedUri.get());
        assertEquals("7", parameters.get("forecast_days"));
        assertEquals("24", parameters.get("forecast_hours"));
        assertEquals("Europe/Budapest", parameters.get("timezone"));
        assertEquals("temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,precipitation,rain,snowfall,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day", parameters.get("current"));
        assertEquals("temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,precipitation,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m", parameters.get("hourly"));
        assertEquals("weather_code,temperature_2m_min,temperature_2m_max,apparent_temperature_min,apparent_temperature_max,precipitation_probability_max,precipitation_sum,rain_sum,snowfall_sum,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,sunrise,sunset,daylight_duration,sunshine_duration", parameters.get("daily"));
    }

    @Test
    void propagatesProviderHttpStatusesAndTimeout() {
        for (int status : new int[]{400, 429, 500}) {
            ExternalHttpExecutor failingExecutor = request -> {
                throw new ExternalApiException(ExternalApiException.REJECTED, "provider failure", status);
            };
            ExternalApiException exception = assertThrows(ExternalApiException.class,
                    () -> new OpenMeteoClient(failingExecutor, URI.create("http://stub"), URI.create("http://stub"), Duration.ofSeconds(1), new Gson()).getWeather(1, 2, "auto"));
            assertEquals(status, exception.getHttpStatus());
        }
        ExternalHttpExecutor timingOutExecutor = request -> {
            throw new ExternalApiException(ExternalApiException.TIMEOUT, "provider timeout");
        };
        assertEquals(ExternalApiException.TIMEOUT, assertThrows(ExternalApiException.class,
                () -> new OpenMeteoClient(timingOutExecutor, URI.create("http://stub"), URI.create("http://stub"), Duration.ofSeconds(1), new Gson()).getWeather(1, 2, "auto")).getCode());
    }

    private void assertInvalid(String body) {
        assertEquals(ExternalApiException.INVALID_RESPONSE, assertThrows(ExternalApiException.class, () -> client(body).getWeather(1, 2, "auto")).getCode());
    }

    private OpenMeteoClient client(String body) {
        ExternalHttpExecutor executor = request -> new ExternalHttpResponse(200, java.util.Map.of(), body);
        return new OpenMeteoClient(executor, URI.create("http://stub"), URI.create("http://stub"), Duration.ofSeconds(1), new Gson());
    }

    @Test
    void preservesNullOptionalMeasurements() {
        String body = fullForecast(1).replace("\"precipitation_probability\":[6]", "\"precipitation_probability\":[null]")
                .replace("\"precipitation_sum\":[7,8]", "\"precipitation_sum\":[null,8]");
        assertEquals(null, client(body).getWeather(1, 2, "auto").hourly().getFirst().precipitationProbability());
        assertEquals(null, client(body).getWeather(1, 2, "auto").daily().getFirst().precipitationSum());
    }

    private String fullForecast(int hourlyEntries) {
        List<String> times = new ArrayList<>();
        for (int index = 0; index < hourlyEntries; index++) {
            times.add("2026-01-01T" + String.format("%02d", index % 24) + ":00");
        }
        String hourlyTimes = quoted(times);
        String hourlyNumbers = repeated(hourlyEntries, "1");
        return "{\"current\":{\"temperature_2m\":1,\"apparent_temperature\":2,\"relative_humidity_2m\":3,\"weather_code\":4,\"precipitation\":5,\"rain\":6,\"snowfall\":7,\"cloud_cover\":8,\"pressure_msl\":9,\"wind_speed_10m\":10,\"wind_direction_10m\":11,\"wind_gusts_10m\":12,\"is_day\":1},"
                + "\"hourly\":{\"time\":" + hourlyTimes + ",\"temperature_2m\":" + hourlyNumbers + ",\"apparent_temperature\":" + hourlyNumbers + ",\"relative_humidity_2m\":" + hourlyNumbers + ",\"precipitation_probability\":" + repeated(hourlyEntries, "6") + ",\"precipitation\":" + hourlyNumbers + ",\"weather_code\":" + hourlyNumbers + ",\"cloud_cover\":" + hourlyNumbers + ",\"wind_speed_10m\":" + hourlyNumbers + ",\"wind_direction_10m\":" + hourlyNumbers + ",\"wind_gusts_10m\":" + hourlyNumbers + "},"
                + "\"daily\":{\"time\":[\"2026-01-01\",\"2026-01-02\"],\"weather_code\":[1,2],\"temperature_2m_min\":[1,2],\"temperature_2m_max\":[3,4],\"apparent_temperature_min\":[1,2],\"apparent_temperature_max\":[3,4],\"precipitation_probability_max\":[5,6],\"precipitation_sum\":[7,8],\"rain_sum\":[7,8],\"snowfall_sum\":[0,0],\"wind_speed_10m_max\":[9,10],\"wind_gusts_10m_max\":[11,12],\"wind_direction_10m_dominant\":[13,14],\"sunrise\":[\"2026-01-01T07:00\",\"2026-01-02T07:01\"],\"sunset\":[\"2026-01-01T16:00\",\"2026-01-02T16:01\"],\"daylight_duration\":[36000,36001],\"sunshine_duration\":[18000,18001]}}";
    }

    private String quoted(List<String> values) {
        return "[" + values.stream().map(value -> "\"" + value + "\"").collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    private String repeated(int count, String value) {
        return "[" + java.util.stream.IntStream.range(0, count).mapToObj(index -> value).collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    private Map<String, String> queryParameters(URI uri) {
        Map<String, String> parameters = new LinkedHashMap<>();
        for (String pair : uri.getRawQuery().split("&")) {
            String[] keyValue = pair.split("=", 2);
            parameters.put(java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8),
                    java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
        }
        return parameters;
    }
}
