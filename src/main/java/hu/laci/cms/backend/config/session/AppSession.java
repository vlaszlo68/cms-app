package hu.laci.cms.backend.config.session;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Store-neutral application session model.
 */
public class AppSession {

    private final String id;
    private AuthenticatedUser authenticatedUser;
    private String csrfToken;
    private final Map<String, AppSessionAttribute> attributes = new HashMap<>();
    private final Instant createdAt;
    private Instant lastAccessedAt;
    private Instant expiresAt;
    private boolean invalidated;

    /**
     * Creates an application session model.
     *
     * @param id external session id known by the store
     * @param authenticatedUser authenticated user snapshot, or null
     * @param csrfToken CSRF token, or null
     * @param createdAt creation timestamp
     * @param lastAccessedAt last access timestamp
     * @param expiresAt expiration timestamp
     */
    public AppSession(String id, AuthenticatedUser authenticatedUser, String csrfToken,
                      Instant createdAt, Instant lastAccessedAt, Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.authenticatedUser = authenticatedUser;
        this.csrfToken = csrfToken;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.lastAccessedAt = Objects.requireNonNull(lastAccessedAt, "lastAccessedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public String getId() {
        return id;
    }

    public Optional<AuthenticatedUser> getAuthenticatedUser() {
        return Optional.ofNullable(authenticatedUser);
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public Map<String, AppSessionAttribute> getAttributes() {
        return attributes;
    }

    public Optional<AppSessionAttribute> getAttribute(String name) {
        AppSessionAttribute attribute = attributes.get(name);
        if (attribute == null || attribute.isDeleted()) {
            return Optional.empty();
        }
        return Optional.of(attribute);
    }

    public void putAttribute(AppSessionAttribute attribute) {
        attributes.put(attribute.getName(), attribute);
    }

    public void removeAttribute(String name) {
        AppSessionAttribute attribute = attributes.get(name);
        if (attribute != null) {
            attribute.markDeleted();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = Objects.requireNonNull(lastAccessedAt, "lastAccessedAt must not be null");
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public boolean isInvalidated() {
        return invalidated;
    }

    public void setInvalidated(boolean invalidated) {
        this.invalidated = invalidated;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
