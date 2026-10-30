package hu.laci.cms.backend.service.weather.openmeteo;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hu.laci.cms.backend.service.external.http.ExternalApiException;
import hu.laci.cms.backend.service.external.http.ExternalHttpExecutor;
import hu.laci.cms.backend.service.external.http.ExternalHttpResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenMeteoClientTest {
    private HttpServer server;

    @org.junit.jupiter.api.AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }
    @Test
    void mapsForecastAndGeocodingAndHandlesNoResults() {
        OpenMeteoClient forecast = client("{\"current\":{\"temperature_2m\":1,\"apparent_temperature\":2,\"relative_humidity_2m\":3,\"wind_speed_10m\":4,\"weather_code\":5},\"daily\":{\"time\":[\"2026-01-01\"],\"weather_code\":[5],\"temperature_2m_min\":[0],\"temperature_2m_max\":[2],\"precipitation_probability_max\":[10]}}");
        assertEquals(1, forecast.getWeather(1, 2, "auto").daily().size());
        OpenMeteoClient geocoding = client("{\"results\":[{\"id\":1,\"name\":\"Budapest\",\"latitude\":47.5,\"longitude\":19.0,\"timezone\":\"Europe/Budapest\"}]}");
        assertEquals("Budapest", geocoding.searchLocations("Budapest", "en").getFirst().name());
        assertEquals(0, client("{}").searchLocations("None", "en").size());
    }

    @Test
    void translatesMalformedVendorResponsesToControlledError() {
        assertInvalid("{\"current\":{\"temperature_2m\":1,\"apparent_temperature\":2,\"relative_humidity_2m\":3,\"wind_speed_10m\":4,\"weather_code\":5},\"daily\":{\"time\":[\"2026-01-01\"],\"weather_code\":[{}],\"temperature_2m_min\":[0],\"temperature_2m_max\":[2],\"precipitation_probability_max\":[10]}}");
        assertEquals(ExternalApiException.INVALID_RESPONSE, assertThrows(ExternalApiException.class, () -> client("{\"results\":[{\"name\":\"Budapest\"}]}").searchLocations("Budapest", "en")).getCode());
        assertInvalid("not-json");
    }

    @Test
    void rejectsMismatchedDailyArraysAndPropagatesProviderFailures() {
        assertInvalid("{\"current\":{\"temperature_2m\":1,\"apparent_temperature\":2,\"relative_humidity_2m\":3,\"wind_speed_10m\":4,\"weather_code\":5},\"daily\":{\"time\":[\"2026-01-01\"],\"weather_code\":[],\"temperature_2m_min\":[0],\"temperature_2m_max\":[2],\"precipitation_probability_max\":[10]}}");
        ExternalHttpExecutor rateLimited = request -> { throw new ExternalApiException(ExternalApiException.RATE_LIMITED, "hidden", 429); };
        assertEquals(ExternalApiException.RATE_LIMITED, assertThrows(ExternalApiException.class, () -> new OpenMeteoClient(rateLimited, URI.create("http://stub"), URI.create("http://stub"), Duration.ofSeconds(1), new Gson()).getWeather(1, 2, "auto")).getCode());
    }

    @Test
    void propagatesProviderHttpStatusesAndTimeoutFromLocalStub() throws Exception {
        start(exchange -> write(exchange, statusFor(exchange), "vendor body must not escape"));
        for (int status : new int[]{400, 429, 500}) {
            ExternalApiException exception = assertThrows(ExternalApiException.class,
                    () -> httpClient().getWeather(status, 2, "auto"));
            assertEquals(status, exception.getHttpStatus());
        }
        server.stop(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> { try { Thread.sleep(200); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); } write(exchange, 200, "{}"); });
        server.start();
        assertEquals(ExternalApiException.TIMEOUT, assertThrows(ExternalApiException.class,
                () -> httpClient().getWeather(1, 2, "auto")).getCode());
    }

    private void assertInvalid(String body) {
        assertEquals(ExternalApiException.INVALID_RESPONSE, assertThrows(ExternalApiException.class, () -> client(body).getWeather(1, 2, "auto")).getCode());
    }

    private OpenMeteoClient client(String body) {
        ExternalHttpExecutor executor = request -> new ExternalHttpResponse(200, java.util.Map.of(), body);
        return new OpenMeteoClient(executor, URI.create("http://stub"), URI.create("http://stub"), Duration.ofSeconds(1), new Gson());
    }

    private OpenMeteoClient httpClient() {
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        return new OpenMeteoClient(new hu.laci.cms.backend.service.external.http.JavaExternalHttpExecutor(Duration.ofSeconds(1)), base, base, Duration.ofMillis(30), new Gson());
    }

    private int statusFor(HttpExchange exchange) {
        String latitude = exchange.getRequestURI().getQuery().replaceAll(".*latitude=([0-9]+).*", "$1");
        return Integer.parseInt(latitude);
    }

    private void start(com.sun.net.httpserver.HttpHandler handler) throws java.io.IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
    }

    private void write(HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
