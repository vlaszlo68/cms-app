package hu.laci.cms.backend.dto.auth;

public class LoginRequest {

    private String loginName;
    private String password;

    public LoginRequest() {
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
