package hu.laci.cms.backend.config.security;

/**
 * Immutable password policy configuration resolved from servlet context parameters.
 * <p>
 * Service-layer validators consume this value object so password rules stay
 * centralized and independent from servlet APIs.
 */
public final class PasswordPolicyConfig {

    public static final int DEFAULT_MIN_LENGTH = 8;

    private final int minLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSpecial;

    public PasswordPolicyConfig(int minLength, boolean requireUppercase, boolean requireLowercase,
                                boolean requireDigit, boolean requireSpecial) {
        this.minLength = minLength;
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireDigit = requireDigit;
        this.requireSpecial = requireSpecial;
    }

    public static PasswordPolicyConfig defaults() {
        return new PasswordPolicyConfig(DEFAULT_MIN_LENGTH, true, true, true, true);
    }

    public int getMinLength() {
        return minLength;
    }

    public boolean isRequireUppercase() {
        return requireUppercase;
    }

    public boolean isRequireLowercase() {
        return requireLowercase;
    }

    public boolean isRequireDigit() {
        return requireDigit;
    }

    public boolean isRequireSpecial() {
        return requireSpecial;
    }
}
