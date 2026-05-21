package hu.laci.cms.backend.dto.auth;

import hu.laci.cms.backend.model.user.UserRole;

import java.io.Serializable;

/**
 * Lightweight authenticated user stored in the HTTP session.
 * <p>
 * It intentionally contains only the identity data needed by request handling,
 * not the full persistence {@code User} entity and not the password hash.
 */
public class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String loginName;
    private final String email;
    private final UserRole role;

    /**
     * Creates a session-safe authenticated user snapshot.
     *
     * @param id user id
     * @param loginName login name
     * @param email email address
     */
    public AuthenticatedUser(Long id, String loginName, String email, UserRole role) {
        this.id = id;
        this.loginName = loginName;
        this.email = email;
        this.role = role;
    }

    /**
     * Returns the user id.
     *
     * @return user id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the login name.
     *
     * @return login name
     */
    public String getLoginName() {
        return loginName;
    }

    /**
     * Returns the email address.
     *
     * @return email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the user role.
     *
     * @return user role
     */
    public UserRole getRole() {
        return role;
    }
}
