package hu.laci.cms.backend.dto.user;

import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.UserRole;

/**
 * API response DTO for user management endpoints.
 * <p>
 * It intentionally does not expose password hashes or plaintext passwords.
 */
public class UserResponse {

    private final Long id;
    private final String loginName;
    private final String userName;
    private final String emailAddress;
    private final UserRole role;
    private final Boolean active;
    private final RegistrationState registrationStatus;
    private final String createdAt;
    private final String updatedAt;

    /**
     * Creates a user response.
     *
     * @param id user id
     * @param loginName login name
     * @param userName display/user name
     * @param emailAddress email address
     * @param role user role
     * @param active active flag
     * @param registrationStatus registration lifecycle status
     * @param createdAt creation timestamp
     * @param updatedAt last update timestamp
     */
    public UserResponse(Long id, String loginName, String userName, String emailAddress, UserRole role,
                        Boolean active, RegistrationState registrationStatus, String createdAt, String updatedAt) {
        this.id = id;
        this.loginName = loginName;
        this.userName = userName;
        this.emailAddress = emailAddress;
        this.role = role;
        this.active = active;
        this.registrationStatus = registrationStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
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

    public UserRole getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }

    public RegistrationState getRegistrationStatus() {
        return registrationStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
