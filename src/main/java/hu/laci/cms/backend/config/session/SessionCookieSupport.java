package hu.laci.cms.backend.config.session;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;

/**
 * Reads and writes the application session cookie for external session stores.
 */
public class SessionCookieSupport {

    private final AppSessionConfig config;

    /**
     * Creates cookie support for the given configuration.
     *
     * @param config session configuration
     */
    public SessionCookieSupport(AppSessionConfig config) {
        this.config = config;
    }

    /**
     * Finds the configured session cookie value.
     *
     * @param request HTTP request
     * @return cookie value when present
     */
    public Optional<String> readSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> config.getCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    /**
     * Adds a session cookie header.
     *
     * @param response HTTP response
     * @param sessionId session id to expose to the browser
     */
    public void addSessionCookie(HttpServletResponse response, String sessionId) {
        response.addHeader("Set-Cookie", buildCookieHeader(sessionId, -1));
    }

    /**
     * Adds a session cookie deletion header.
     *
     * @param response HTTP response
     */
    public void clearSessionCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookieHeader("", 0));
    }

    private String buildCookieHeader(String value, int maxAge) {
        StringBuilder header = new StringBuilder();
        header.append(config.getCookieName()).append("=").append(value == null ? "" : value);
        header.append("; Path=/");
        header.append("; HttpOnly");
        header.append("; SameSite=").append(config.getCookieSameSite());
        if (config.isCookieSecure()) {
            header.append("; Secure");
        }
        if (maxAge >= 0) {
            header.append("; Max-Age=").append(maxAge);
        }
        return header.toString();
    }
}
