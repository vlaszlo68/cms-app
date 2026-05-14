package hu.laci.cms.backend.dto.auth;

import java.io.Serializable;

public class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String loginName;
    private final String email;

    public AuthenticatedUser(Long id, String loginName, String email) {
        this.id = id;
        this.loginName = loginName;
        this.email = email;
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
