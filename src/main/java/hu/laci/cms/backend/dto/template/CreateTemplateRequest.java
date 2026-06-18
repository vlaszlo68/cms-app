package hu.laci.cms.backend.dto.template;

/**
 * Request DTO for creating a template configuration.
 */
public class CreateTemplateRequest extends TemplateRequestBase {

    public CreateTemplateRequest() {
    }

    public CreateTemplateRequest(String code, String name, String description, Long previewImageMediaId,
                                 Boolean active) {
        super(code, name, description, previewImageMediaId, active);
    }
}
