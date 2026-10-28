package hu.laci.cms.backend.dto.auth;

/** Public authentication state returned after login and during SPA session restoration. */
public record PublicAuthStateResponse(boolean authenticated, PublicUserResponse user, String csrfToken) {
    public static PublicAuthStateResponse anonymous() { return new PublicAuthStateResponse(false, null, null); }
    public static PublicAuthStateResponse authenticated(AuthenticatedUser user, String token) {
        return new PublicAuthStateResponse(true, new PublicUserResponse(user), token);
    }
}
