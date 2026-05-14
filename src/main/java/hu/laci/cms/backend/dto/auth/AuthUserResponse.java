package hu.laci.cms.backend.dto.auth;

public class AuthUserResponse {

    private final Long id;
    private final String loginName;
    private final String email;

    public AuthUserResponse(Long id, String loginName, String email) {
        this.id = id;
        this.loginName = loginName;
        this.email = email;
    }

    public AuthUserResponse(AuthenticatedUser authenticatedUser) {
        this(authenticatedUser.getId(), authenticatedUser.getLoginName(), authenticatedUser.getEmail());
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
}
