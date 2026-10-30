package hu.laci.cms.backend.service.external.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Typed transport request for a trusted provider adapter. */
public record ExternalHttpRequest(URI uri, String method, Map<String, String> headers, String body, Duration timeout) {
    public ExternalHttpRequest { Objects.requireNonNull(uri, "uri"); Objects.requireNonNull(method, "method"); headers = headers == null ? Map.of() : Map.copyOf(headers); Objects.requireNonNull(timeout, "timeout"); }
    /** Creates a GET request with no body. */
    public static ExternalHttpRequest get(URI uri, Duration timeout) { return new ExternalHttpRequest(uri, "GET", Map.of(), null, timeout); }
}
