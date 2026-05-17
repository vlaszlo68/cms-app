package hu.laci.cms.backend.servlet.support;

import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility methods and constants for session-based CSRF token handling.
 */
public final class CsrfTokenSupport {

    /**
     * HTTP session attribute name that stores the CSRF token.
     */
    public static final String SESSION_ATTRIBUTE = "csrfToken";
    /**
     * Request header name expected by {@code CsrfFilter}.
     */
    public static final String HEADER_NAME = "X-CSRF-Token";

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfTokenSupport() {
    }

    /**
     * Creates a new URL-safe random CSRF token.
     *
     * @return generated token
     */
    public static String createToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Returns the session's existing CSRF token or creates and stores a new one.
     *
     * @param session authenticated HTTP session
     * @return existing or newly created CSRF token
     */
    public static String ensureToken(HttpSession session) {
        Object existingToken = session.getAttribute(SESSION_ATTRIBUTE);
        if (existingToken instanceof String token && !token.isBlank()) {
            return token;
        }

        String token = createToken();
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }
}
