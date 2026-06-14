package hu.laci.cms.backend.dto.user;

import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.UserRole;

/**
 * Request DTO for updating a user.
 */
public class UpdateUserRequest extends UserRequestBase {

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
        super(loginName, userName, emailAddress, password, role, active, registrationStatus);
    }
}
