package hu.laci.cms.backend.config.e2e;

/**
 * Controls local public-auth end-to-end fixtures.
 *
 * <p>The fixtures are deliberately opt-in so deployed environments never gain
 * known accounts or executable demonstration functions by default.</p>
 */
public final class E2ePublicAuthFixtures {

    /** Environment variable that enables the local fixture set. */
    public static final String ENABLED_ENVIRONMENT_VARIABLE = "CMS_E2E_PUBLIC_AUTH_ENABLED";

    private E2ePublicAuthFixtures() {
    }

    /**
     * Returns whether the local fixture set was explicitly enabled at process startup.
     *
     * @return {@code true} only for the exact value {@code true}, ignoring case
     */
    public static boolean isEnabled() {
        return "true".equalsIgnoreCase(System.getenv(ENABLED_ENVIRONMENT_VARIABLE));
    }
}
