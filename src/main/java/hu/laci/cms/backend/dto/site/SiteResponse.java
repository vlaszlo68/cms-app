package hu.laci.cms.backend.dto.site;

/**
 * Administrator-facing Site representation.
 */
public class SiteResponse {

    private final Long id;
    private final String code;
    private final String name;
    private final boolean active;
    private final String createdAt;
    private final String updatedAt;

    /**
     * Creates the administrator-facing Site response.
     *
     * @param id persisted Site id
     * @param code stable machine-readable code
     * @param name administrator-facing name
     * @param active whether public APIs may serve the Site
     * @param createdAt ISO-8601 creation timestamp
     * @param updatedAt ISO-8601 update timestamp
     */
    public SiteResponse(Long id, String code, String name, boolean active, String createdAt, String updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** @return persisted Site id */
    public Long getId() { return id; }
    /** @return stable machine-readable code */
    public String getCode() { return code; }
    /** @return administrator-facing name */
    public String getName() { return name; }
    /** @return whether public APIs may serve the Site */
    public boolean isActive() { return active; }
    /** @return ISO-8601 creation timestamp */
    public String getCreatedAt() { return createdAt; }
    /** @return ISO-8601 update timestamp */
    public String getUpdatedAt() { return updatedAt; }
}
