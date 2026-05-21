package hu.laci.cms.backend.dto.auth;

import hu.laci.cms.backend.model.user.UserRole;

/**
 * Authentication response DTO returned by login and session-restore endpoints.
 */
public class AuthUserResponse {

    private final Long id;
    private final String loginName;
    private final String email;
    private final UserRole role;
    private final String csrfToken;

    /**
     * Creates an authentication response.
     *
     * @param id user id
     * @param loginName login name
     * @param email email address
     * @param csrfToken CSRF token to use for state-changing requests
     */
    public AuthUserResponse(Long id, String loginName, String email, UserRole role, String csrfToken) {
        this.id = id;
        this.loginName = loginName;
        this.email = email;
        this.role = role;
        this.csrfToken = csrfToken;
    }

    /**
     * Creates an authentication response from a session user snapshot.
     *
     * @param authenticatedUser authenticated user
     * @param csrfToken CSRF token
     */
    public AuthUserResponse(AuthenticatedUser authenticatedUser, String csrfToken) {
        this(authenticatedUser.getId(), authenticatedUser.getLoginName(), authenticatedUser.getEmail(),
                authenticatedUser.getRole(), csrfToken);
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

    /**
     * Returns the CSRF token.
     *
     * @return CSRF token
     */
    public String getCsrfToken() {
        return csrfToken;
    }
}
