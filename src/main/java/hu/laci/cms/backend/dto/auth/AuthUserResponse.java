package hu.laci.cms.backend.dto.auth;

public class AuthUserResponse {

    private final Long id;
    private final String loginName;
    private final String email;
    private final String csrfToken;

    public AuthUserResponse(Long id, String loginName, String email, String csrfToken) {
        this.id = id;
        this.loginName = loginName;
        this.email = email;
        this.csrfToken = csrfToken;
    }

    public AuthUserResponse(AuthenticatedUser authenticatedUser, String csrfToken) {
        this(authenticatedUser.getId(), authenticatedUser.getLoginName(), authenticatedUser.getEmail(), csrfToken);
    }

    public Long getId() {
        return id;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getEmail() {
        return email;
    }

    public String getCsrfToken() {
        return csrfToken;
    }
}
