package hu.laci.cms.backend.service.security;

import hu.laci.cms.backend.config.security.PasswordPolicyConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates plain-text passwords against a configured password policy.
 * <p>
 * The validator returns stable error codes so API clients can present useful
 * validation feedback without parsing human-readable text.
 */
public class PasswordPolicyValidator {

    public static final String TOO_SHORT = "TOO_SHORT";
    public static final String MISSING_UPPERCASE = "MISSING_UPPERCASE";
    public static final String MISSING_LOWERCASE = "MISSING_LOWERCASE";
    public static final String MISSING_DIGIT = "MISSING_DIGIT";
    public static final String MISSING_SPECIAL = "MISSING_SPECIAL";

    private final PasswordPolicyConfig config;

    public PasswordPolicyValidator(PasswordPolicyConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        String value = password == null ? "" : password;

        if (value.length() < config.getMinLength()) {
            errors.add(TOO_SHORT);
        }
        if (config.isRequireUppercase() && value.chars().noneMatch(Character::isUpperCase)) {
            errors.add(MISSING_UPPERCASE);
        }
        if (config.isRequireLowercase() && value.chars().noneMatch(Character::isLowerCase)) {
            errors.add(MISSING_LOWERCASE);
        }
        if (config.isRequireDigit() && value.chars().noneMatch(Character::isDigit)) {
            errors.add(MISSING_DIGIT);
        }
        if (config.isRequireSpecial() && value.chars().noneMatch(PasswordPolicyValidator::isSpecial)) {
            errors.add(MISSING_SPECIAL);
        }

        return errors;
    }

    private static boolean isSpecial(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }
}
