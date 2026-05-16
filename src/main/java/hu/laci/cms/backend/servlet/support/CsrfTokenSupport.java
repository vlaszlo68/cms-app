package hu.laci.cms.backend.servlet.support;

import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfTokenSupport {

    public static final String SESSION_ATTRIBUTE = "csrfToken";
    public static final String HEADER_NAME = "X-CSRF-Token";

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfTokenSupport() {
    }

    public static String createToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

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
