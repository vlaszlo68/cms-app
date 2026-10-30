package hu.laci.cms.backend.service.external.http;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Builds provider URLs from a trusted configured base, fixed path, and safely encoded query values. */
public final class TrustedUriBuilder {
    private TrustedUriBuilder() { }
    /** Builds a URI without permitting callers to supply a host or arbitrary endpoint. */
    public static URI build(URI trustedBase, String fixedPath, Map<String, String> query) {
        if (trustedBase == null || trustedBase.getHost() == null || !fixedPath.startsWith("/")) throw new IllegalArgumentException("Invalid trusted provider URI.");
        String values = query.entrySet().stream().filter(entry -> entry.getValue() != null)
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue())).reduce((left, right) -> left + "&" + right).orElse("");
        return URI.create(trustedBase.getScheme() + "://" + trustedBase.getAuthority() + fixedPath + (values.isEmpty() ? "" : "?" + values));
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
