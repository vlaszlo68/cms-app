package hu.laci.cms.backend.dto.site;

/**
 * Request DTO for updating a Site selected by its current code.
 */
public class UpdateSiteRequest extends SiteRequestBase {

    /** Creates an empty request for JSON deserialization. */
    public UpdateSiteRequest() {
    }

    /**
     * Creates a Site update request.
     *
     * @param code replacement machine-readable code
     * @param name replacement administrator-facing name
     * @param active whether public APIs may serve the Site
     */
    public UpdateSiteRequest(String code, String name, Boolean active) {
        super(code, name, active);
    }

    public UpdateSiteRequest(String code, String name, Boolean active, String themeCode) {
        super(code, name, active, themeCode);
    }
}
