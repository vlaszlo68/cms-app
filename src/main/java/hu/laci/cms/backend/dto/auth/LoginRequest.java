package hu.laci.cms.backend.dto.auth;

/**
 * JSON request DTO for {@code POST /api/auth/login}.
 */
public class LoginRequest {

    private String loginName;
    private String password;
    private String captchaId;
    private String captchaAnswer;

    /**
     * Creates an empty request DTO for Gson deserialization.
     */
    public LoginRequest() {
    }

    /**
     * Returns the submitted login name.
     *
     * @return login name
     */
    public String getLoginName() {
        return loginName;
    }

    /**
     * Sets the submitted login name.
     *
     * @param loginName login name
     */
    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    /**
     * Returns the submitted password.
     *
     * @return plain-text password from the request body
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the submitted password.
     *
     * @param password plain-text password from the request body
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaAnswer() {
        return captchaAnswer;
    }

    public void setCaptchaAnswer(String captchaAnswer) {
        this.captchaAnswer = captchaAnswer;
    }
}
