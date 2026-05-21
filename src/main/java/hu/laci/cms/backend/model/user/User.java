package hu.laci.cms.backend.model.user;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

@DbTable("users")
public class User extends AuditableEntity {

    @DbColumn("username")
    private String userName;

    @DbColumn("login_name")
    private String loginName;

    @DbColumn("email_address")
    private String emailAddress;

    @DbColumn("password_hash")
    private String passwordHash;

    @DbColumn("role")
    private UserRole role = UserRole.USER;

    @DbColumn("active")
    private Boolean active = Boolean.TRUE;

    public User() {
    }

    public User(Long id, String userName, String loginName, String emailAddress, String passwordHash) {
        this(id, userName, loginName, emailAddress, passwordHash, UserRole.USER, Boolean.TRUE);
    }

    public User(Long id, String userName, String loginName, String emailAddress, String passwordHash,
                UserRole role, Boolean active) {
        setId(id);
        this.userName = userName;
        this.loginName = loginName;
        this.emailAddress = emailAddress;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
