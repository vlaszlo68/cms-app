package hu.laci.cms.backend.dto.site;

/**
 * Request DTO for creating a Site.
 */
public class CreateSiteRequest extends SiteRequestBase {

    /** Creates an empty request for JSON deserialization. */
    public CreateSiteRequest() {
    }

    /**
     * Creates a Site creation request.
     *
     * @param code stable machine-readable code
     * @param name administrator-facing name
     * @param active whether public APIs may serve the Site
     */
    public CreateSiteRequest(String code, String name, Boolean active) {
        super(code, name, active);
    }
}
