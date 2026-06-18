package hu.laci.cms.backend.dto.template;

/**
 * Request DTO for updating a template configuration.
 */
public class UpdateTemplateRequest extends TemplateRequestBase {

    public UpdateTemplateRequest() {
    }

    public UpdateTemplateRequest(String code, String name, String description, Long previewImageMediaId,
                                 Boolean active) {
        super(code, name, description, previewImageMediaId, active);
    }
}
