package hu.laci.cms.backend.dto.auth;

import hu.laci.cms.backend.model.user.UserRole;

/** Minimal public-safe representation of an authenticated session user. */
public record PublicUserResponse(Long id, String email, UserRole role) {
    public PublicUserResponse(AuthenticatedUser user) { this(user.getId(), user.getEmail(), user.getRole()); }
}
