package hu.laci.cms.backend.dto.auth;

/**
 * Public registration request DTO.
 */
public class RegisterRequest {

    private String loginName;
    private String userName;
    private String emailAddress;
    private String password;
    private String captchaId;
    private String captchaAnswer;
    private String captchaHoneypot;

    public RegisterRequest() {
    }

    public RegisterRequest(String loginName, String userName, String emailAddress, String password,
                           String captchaId, String captchaAnswer) {
        this.loginName = loginName;
        this.userName = userName;
        this.emailAddress = emailAddress;
        this.password = password;
        this.captchaId = captchaId;
        this.captchaAnswer = captchaAnswer;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPassword() {
        return password;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public String getCaptchaAnswer() {
        return captchaAnswer;
    }

    public String getCaptchaHoneypot() {
        return captchaHoneypot;
    }
}
