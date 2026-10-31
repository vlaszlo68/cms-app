package hu.laci.cms.backend.service.external.http;

import java.util.List;
import java.util.Map;

/** Successful provider transport response, retained as text for provider-owned mapping. */
public record ExternalHttpResponse(int statusCode, Map<String, List<String>> headers, String body) {
    public ExternalHttpResponse { headers = headers == null ? Map.of() : Map.copyOf(headers); body = body == null ? "" : body; }
}
