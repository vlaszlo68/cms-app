package hu.laci.cms.model;

public class User {

    private Long id;
    private String userName;
    private String loginName;
    private String emailAddress;
    private String passwordHash;

    public User() {
    }

    public User(Long id, String userName, String loginName, String emailAddress, String passwordHash) {
        this.id = id;
        this.userName = userName;
        this.loginName = loginName;
        this.emailAddress = emailAddress;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
