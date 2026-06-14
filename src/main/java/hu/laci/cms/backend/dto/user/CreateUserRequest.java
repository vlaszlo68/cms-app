package hu.laci.cms.backend.dto.user;

import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.UserRole;

/**
 * Request DTO for creating a user.
 */
public class CreateUserRequest extends UserRequestBase {

    /**
     * Creates an empty request for JSON deserialization.
     */
    public CreateUserRequest() {
    }

    /**
     * Creates a request instance.
     *
     * @param loginName login name
     * @param userName user/display name
     * @param emailAddress email address
     * @param password plaintext password to hash
     * @param role user role
     * @param active active flag
     * @param registrationStatus registration status, or null for service default
     */
    public CreateUserRequest(String loginName, String userName, String emailAddress, String password, UserRole role,
                             Boolean active, RegistrationState registrationStatus) {
        super(loginName, userName, emailAddress, password, role, active, registrationStatus);
    }
}
