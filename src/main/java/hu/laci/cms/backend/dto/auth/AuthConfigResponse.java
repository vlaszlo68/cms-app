package hu.laci.cms.backend.dto.auth;

/**
 * Public authentication UI configuration resolved from backend security settings.
 */
public class AuthConfigResponse {

    private final boolean loginCaptchaEnabled;
    private final boolean registrationCaptchaEnabled;
    private final PasswordPolicyResponse passwordPolicy;

    public AuthConfigResponse(boolean loginCaptchaEnabled, boolean registrationCaptchaEnabled,
                              PasswordPolicyResponse passwordPolicy) {
        this.loginCaptchaEnabled = loginCaptchaEnabled;
        this.registrationCaptchaEnabled = registrationCaptchaEnabled;
        this.passwordPolicy = passwordPolicy;
    }

    public boolean isLoginCaptchaEnabled() {
        return loginCaptchaEnabled;
    }

    public boolean isRegistrationCaptchaEnabled() {
        return registrationCaptchaEnabled;
    }

    public PasswordPolicyResponse getPasswordPolicy() {
        return passwordPolicy;
    }
}
