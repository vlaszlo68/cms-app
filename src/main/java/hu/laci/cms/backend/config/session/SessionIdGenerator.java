package hu.laci.cms.backend.config.session;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates high-entropy session identifiers for external session stores.
 */
public final class SessionIdGenerator {

    private static final int SESSION_ID_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SessionIdGenerator() {
    }

    /**
     * Generates a URL-safe session id.
     *
     * @return generated session id
     */
    public static String generate() {
        byte[] bytes = new byte[SESSION_ID_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
