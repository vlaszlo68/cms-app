package hu.laci.cms.backend.dto.auth;

import hu.laci.cms.backend.config.security.PasswordPolicyConfig;

/**
 * Public password policy DTO used by auth UI configuration responses.
 */
public class PasswordPolicyResponse {

    private final int minLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSpecial;

    public PasswordPolicyResponse(PasswordPolicyConfig passwordPolicyConfig) {
        this.minLength = passwordPolicyConfig.getMinLength();
        this.requireUppercase = passwordPolicyConfig.isRequireUppercase();
        this.requireLowercase = passwordPolicyConfig.isRequireLowercase();
        this.requireDigit = passwordPolicyConfig.isRequireDigit();
        this.requireSpecial = passwordPolicyConfig.isRequireSpecial();
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
