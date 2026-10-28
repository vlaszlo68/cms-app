package hu.laci.cms.backend.dto.auth;

/** Credentials accepted by the site-aware public authentication endpoint. */
public class PublicLoginRequest {
    private String email;
    private String password;
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
