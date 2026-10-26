package hu.laci.cms.backend.dto.site;

/**
 * Shared editable fields for Site create and update requests.
 */
public abstract class SiteRequestBase {

    private String code;
    private String name;
    private Boolean active;

    /** Creates an empty request for JSON deserialization. */
    protected SiteRequestBase() {
    }

    /**
     * Creates a request with all editable Site fields.
     *
     * @param code stable machine-readable code
     * @param name administrator-facing name
     * @param active whether public APIs may serve the Site
     */
    protected SiteRequestBase(String code, String name, Boolean active) {
        this.code = code;
        this.name = name;
        this.active = active;
    }

    /** @return requested Site code */
    public String getCode() {
        return code;
    }

    /** @param code requested Site code */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return requested Site name */
    public String getName() {
        return name;
    }

    /** @param name requested Site name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return requested active state, or {@code null} for the default */
    public Boolean getActive() {
        return active;
    }

    /** @param active requested active state, or {@code null} for the default */
    public void setActive(Boolean active) {
        this.active = active;
    }
}
