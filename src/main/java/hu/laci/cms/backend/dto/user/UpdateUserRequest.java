package hu.laci.cms.backend.dto.user;

import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.UserRole;

/**
 * Request DTO for updating a user.
 */
public class UpdateUserRequest {

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
    public UpdateUserRequest() {
    }

    /**
     * Creates a request instance.
     *
     * @param loginName login name
     * @param userName user/display name
     * @param emailAddress email address
     * @param password optional plaintext password to hash
     * @param role user role
     * @param active active flag
     * @param registrationStatus registration status, or null to keep current value
     */
    public UpdateUserRequest(String loginName, String userName, String emailAddress, String password, UserRole role,
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
