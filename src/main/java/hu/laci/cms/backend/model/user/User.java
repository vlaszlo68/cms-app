package hu.laci.cms.backend.model.user;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.BaseEntity;

@DbTable("users")
public class User extends BaseEntity {

    @DbColumn("username")
    private String userName;

    @DbColumn("login_name")
    private String loginName;

    @DbColumn("email_address")
    private String emailAddress;

    @DbColumn("password_hash")
    private String passwordHash;

    public User() {
    }

    public User(Long id, String userName, String loginName, String emailAddress, String passwordHash) {
        setId(id);
        this.userName = userName;
        this.loginName = loginName;
        this.emailAddress = emailAddress;
        this.passwordHash = passwordHash;
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
