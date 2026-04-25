package hu.laci.cms.model;

public class UserFilter {

    private String userName;
    private String loginName;
    private String emailAddress;

    public UserFilter() {
    }

    public UserFilter(String userName, String loginName, String emailAddress) {
        this.userName = userName;
        this.loginName = loginName;
        this.emailAddress = emailAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
