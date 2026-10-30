package hu.laci.cms.backend.config.external;

import hu.laci.cms.backend.config.app.ServletContextParameters;

import javax.servlet.ServletContext;
import java.net.URI;
import java.util.Map;

/** Immutable environment-first configuration for trusted outbound API providers. */
public final class ExternalApiConfig {
    public static final String ENV_CONNECT_TIMEOUT_SECONDS = "EXTERNAL_HTTP_CONNECT_TIMEOUT_SECONDS";
    public static final String ENV_REQUEST_TIMEOUT_SECONDS = "EXTERNAL_HTTP_REQUEST_TIMEOUT_SECONDS";
    public static final String ENV_OPEN_METEO_GEOCODING_BASE_URL = "OPEN_METEO_GEOCODING_BASE_URL";
    public static final String ENV_OPEN_METEO_FORECAST_BASE_URL = "OPEN_METEO_FORECAST_BASE_URL";
    public static final String PARAM_CONNECT_TIMEOUT_SECONDS = "external.http.connectTimeoutSeconds";
    public static final String PARAM_REQUEST_TIMEOUT_SECONDS = "external.http.requestTimeoutSeconds";
    public static final String PARAM_OPEN_METEO_GEOCODING_BASE_URL = "openMeteo.geocoding.baseUrl";
    public static final String PARAM_OPEN_METEO_FORECAST_BASE_URL = "openMeteo.forecast.baseUrl";
    private static final ExternalApiConfig DEFAULT = new ExternalApiConfig(3, 5,
            URI.create("https://geocoding-api.open-meteo.com"), URI.create("https://api.open-meteo.com"));
    private static volatile ExternalApiConfig current = DEFAULT;
    private final int connectTimeoutSeconds;
    private final int requestTimeoutSeconds;
    private final URI openMeteoGeocodingBaseUrl;
    private final URI openMeteoForecastBaseUrl;

    private ExternalApiConfig(int connectTimeoutSeconds, int requestTimeoutSeconds, URI geocoding, URI forecast) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.openMeteoGeocodingBaseUrl = geocoding;
        this.openMeteoForecastBaseUrl = forecast;
    }
    /** Initializes configuration from environment variables, then web.xml, then defaults. */
    public static void initialize(ServletContext context) { current = from(context, System.getenv()); }
    /** Restores defaults for shutdown and focused tests. */
    public static void reset() { current = DEFAULT; }
    /** @return current immutable configuration */
    public static ExternalApiConfig getCurrent() { return current; }
    static ExternalApiConfig from(ServletContext context, Map<String, String> environment) {
        return new ExternalApiConfig(
                positive(value(environment, ENV_CONNECT_TIMEOUT_SECONDS, context, PARAM_CONNECT_TIMEOUT_SECONDS), 3),
                positive(value(environment, ENV_REQUEST_TIMEOUT_SECONDS, context, PARAM_REQUEST_TIMEOUT_SECONDS), 5),
                trustedBase(value(environment, ENV_OPEN_METEO_GEOCODING_BASE_URL, context, PARAM_OPEN_METEO_GEOCODING_BASE_URL), DEFAULT.openMeteoGeocodingBaseUrl),
                trustedBase(value(environment, ENV_OPEN_METEO_FORECAST_BASE_URL, context, PARAM_OPEN_METEO_FORECAST_BASE_URL), DEFAULT.openMeteoForecastBaseUrl));
    }
    private static String value(Map<String, String> env, String envKey, ServletContext context, String parameter) {
        String result = env.get(envKey);
        return result != null && !result.isBlank() ? result : ServletContextParameters.getString(context, parameter);
    }
    private static int positive(String value, int fallback) { try { int parsed = Integer.parseInt(value == null ? "" : value.trim()); return parsed > 0 ? parsed : fallback; } catch (NumberFormatException exception) { return fallback; } }
    private static URI trustedBase(String value, URI fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { URI uri = URI.create(value.trim()); return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null ? uri : fallback; }
        catch (IllegalArgumentException exception) { return fallback; }
    }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public URI getOpenMeteoGeocodingBaseUrl() { return openMeteoGeocodingBaseUrl; }
    public URI getOpenMeteoForecastBaseUrl() { return openMeteoForecastBaseUrl; }
}
