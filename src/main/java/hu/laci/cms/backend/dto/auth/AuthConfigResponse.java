package hu.laci.cms.backend.dto.auth;

/**
 * Public authentication UI configuration resolved from backend security settings.
 */
public class AuthConfigResponse {

    private final boolean loginCaptchaEnabled;
    private final boolean registrationCaptchaEnabled;

    public AuthConfigResponse(boolean loginCaptchaEnabled, boolean registrationCaptchaEnabled) {
        this.loginCaptchaEnabled = loginCaptchaEnabled;
        this.registrationCaptchaEnabled = registrationCaptchaEnabled;
    }

    public boolean isLoginCaptchaEnabled() {
        return loginCaptchaEnabled;
    }

    public boolean isRegistrationCaptchaEnabled() {
        return registrationCaptchaEnabled;
    }
}
