package hu.laci.cms.backend.dto.user;

import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.UserRole;

/**
 * Shared fields for user create and update request DTOs.
 */
public abstract class UserRequestBase {

    private String loginName;
    private String userName;
    private String emailAddress;
    private String password;
    private UserRole role;
    private Boolean active;
    private RegistrationState registrationStatus;

    /**
     * Creates an empty request for JSON deserialization.
     */
    protected UserRequestBase() {
    }

    /**
     * Creates a request instance.
     *
     * @param loginName login name
     * @param userName user/display name
     * @param emailAddress email address
     * @param password plaintext password
     * @param role user role
     * @param active active flag
     * @param registrationStatus registration status
     */
    protected UserRequestBase(String loginName, String userName, String emailAddress, String password, UserRole role,
                              Boolean active, RegistrationState registrationStatus) {
        this.loginName = loginName;
        this.userName = userName;
        this.emailAddress = emailAddress;
        this.password = password;
        this.role = role;
        this.active = active;
        this.registrationStatus = registrationStatus;
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

    public UserRole getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }

    public RegistrationState getRegistrationStatus() {
        return registrationStatus;
    }
}
