package hu.laci.cms.backend.config.session;

import java.time.Instant;
import java.util.Objects;

/**
 * Structured session attribute stored behind the common session abstraction.
 * <p>
 * The payload is JSON so JDBC and future Redis stores can persist the same
 * application-level model without exposing store-specific APIs to servlets.
 */
public class AppSessionAttribute {

    private final String name;
    private final AppSessionAttributeType type;
    private String jsonValue;
    private final Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;

    /**
     * Creates a session attribute.
     *
     * @param name logical attribute name within the session
     * @param type payload type
     * @param jsonValue JSON payload
     * @param createdAt creation timestamp
     * @param updatedAt last update timestamp
     */
    public AppSessionAttribute(String name, AppSessionAttributeType type, String jsonValue,
                               Instant createdAt, Instant updatedAt) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.jsonValue = Objects.requireNonNull(jsonValue, "jsonValue must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public String getName() {
        return name;
    }

    public AppSessionAttributeType getType() {
        return type;
    }

    public String getJsonValue() {
        return jsonValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Replaces the JSON payload and refreshes the update timestamp.
     *
     * @param jsonValue new JSON payload
     * @param updatedAt update timestamp
     */
    public void update(String jsonValue, Instant updatedAt) {
        this.jsonValue = Objects.requireNonNull(jsonValue, "jsonValue must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.deleted = false;
    }

    /**
     * Marks the attribute for removal from the backing store.
     */
    public void markDeleted() {
        this.deleted = true;
    }
}
