package hu.laci.cms.backend.service.external.http;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/** Java 21 {@link HttpClient} executor with a shared client and controlled status/transport failures. */
public final class JavaExternalHttpExecutor implements ExternalHttpExecutor {
    private final HttpClient client;
    public JavaExternalHttpExecutor(Duration connectTimeout) { this(HttpClient.newBuilder().connectTimeout(connectTimeout).build()); }
    public JavaExternalHttpExecutor(HttpClient client) { this.client = Objects.requireNonNull(client, "client"); }
    @Override public ExternalHttpResponse execute(ExternalHttpRequest request) {
        long started = System.nanoTime();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri()).timeout(request.timeout());
            request.headers().forEach(builder::header);
            builder.method(request.method(), request.body() == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(request.body()));
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) return new ExternalHttpResponse(status, response.headers().map(), response.body());
            throw statusException(status);
        } catch (java.net.http.HttpTimeoutException exception) { throw new ExternalApiException(ExternalApiException.TIMEOUT, "External service timed out.");
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new ExternalApiException(ExternalApiException.UNAVAILABLE, "External service is unavailable.");
        } catch (IOException exception) { throw new ExternalApiException(ExternalApiException.UNAVAILABLE, "External service is unavailable.");
        } finally { long ignoredDurationNanos = System.nanoTime() - started; }
    }
    private ExternalApiException statusException(int status) {
        if (status == 401 || status == 403) return new ExternalApiException(ExternalApiException.AUTHENTICATION, "External service authentication failed.", status);
        if (status == 429) return new ExternalApiException(ExternalApiException.RATE_LIMITED, "External service rate limit reached.", status);
        if (status >= 500) return new ExternalApiException(ExternalApiException.UNAVAILABLE, "External service is unavailable.", status);
        return new ExternalApiException(ExternalApiException.REJECTED, "External service rejected the request.", status);
    }
}
