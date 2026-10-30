package hu.laci.cms.backend.service.external.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaExternalHttpExecutorTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void executesGetWithHeadersAndMapsProviderStatuses() throws Exception {
        start(exchange -> {
            if (exchange.getRequestURI().getPath().endsWith("/ok") && (!"GET".equals(exchange.getRequestMethod()) || !"token".equals(exchange.getRequestHeaders().getFirst("X-Test")) || !"Budapest".equals(query(exchange)))) {
                write(exchange, 400, "bad request");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            int status = path.endsWith("/auth") ? 401 : path.endsWith("/forbidden") ? 403 : path.endsWith("/rate") ? 429 : path.endsWith("/down") ? 500 : path.endsWith("/bad") ? 400 : 200;
            write(exchange, status, "response");
        });
        JavaExternalHttpExecutor executor = new JavaExternalHttpExecutor(Duration.ofSeconds(1));
        ExternalHttpRequest success = new ExternalHttpRequest(uri("/ok?name=Budapest"), "GET", Map.of("X-Test", "token"), null, Duration.ofSeconds(1));
        assertEquals("response", executor.execute(success).body());
        assertCode(executor, "/auth", ExternalApiException.AUTHENTICATION, 401);
        assertCode(executor, "/forbidden", ExternalApiException.AUTHENTICATION, 403);
        assertCode(executor, "/rate", ExternalApiException.RATE_LIMITED, 429);
        assertCode(executor, "/down", ExternalApiException.UNAVAILABLE, 500);
        assertCode(executor, "/bad", ExternalApiException.REJECTED, 400);
    }

    @Test
    void mapsTimeoutAndConnectionFailure() throws Exception {
        start(exchange -> {
            try { Thread.sleep(200); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            write(exchange, 200, "slow");
        });
        JavaExternalHttpExecutor executor = new JavaExternalHttpExecutor(Duration.ofSeconds(1));
        assertEquals(ExternalApiException.TIMEOUT, assertThrows(ExternalApiException.class, () -> executor.execute(ExternalHttpRequest.get(uri("/slow"), Duration.ofMillis(20)))).getCode());
        assertEquals(ExternalApiException.UNAVAILABLE, assertThrows(ExternalApiException.class, () -> executor.execute(ExternalHttpRequest.get(URI.create("http://127.0.0.1:1/unavailable"), Duration.ofMillis(100)))).getCode());
    }

    private void assertCode(JavaExternalHttpExecutor executor, String path, String code, int status) {
        ExternalApiException exception = assertThrows(ExternalApiException.class, () -> executor.execute(ExternalHttpRequest.get(uri(path), Duration.ofSeconds(1))));
        assertEquals(code, exception.getCode());
        assertEquals(status, exception.getHttpStatus());
    }

    private void start(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path); }
    private String query(HttpExchange exchange) { return exchange.getRequestURI().getQuery().split("=", 2)[1]; }
    private void write(HttpExchange exchange, int status, String body) throws IOException { byte[] bytes = body.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); exchange.close(); }
}
