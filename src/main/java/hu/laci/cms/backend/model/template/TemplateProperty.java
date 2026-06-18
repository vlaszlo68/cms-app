package hu.laci.cms.backend.model.template;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

/**
 * Query properties available for {@link Template} DAO operations.
 */
public final class TemplateProperty extends AuditableProperty {

    public static final TemplateProperty ID = new TemplateProperty(BaseProperty.ID.getPropertyName());
    public static final TemplateProperty CODE = new TemplateProperty("code");
    public static final TemplateProperty NAME = new TemplateProperty("name");
    public static final TemplateProperty DESCRIPTION = new TemplateProperty("description");
    public static final TemplateProperty PREVIEW_IMAGE_MEDIA_ID = new TemplateProperty("previewImageMediaId");
    public static final TemplateProperty ACTIVE = new TemplateProperty("active");
    public static final TemplateProperty CREATED_AT = new TemplateProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final TemplateProperty UPDATED_AT = new TemplateProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final TemplateProperty CREATED_BY = new TemplateProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final TemplateProperty UPDATED_BY = new TemplateProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private TemplateProperty(String propertyName) {
        super(Template.class, propertyName);
    }
}
