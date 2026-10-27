package hu.laci.cms.backend.model.application;
import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;
/** Query properties for Application. */
public final class ApplicationProperty extends AuditableProperty {
    public static final ApplicationProperty ID = new ApplicationProperty(BaseProperty.ID.getPropertyName());
    public static final ApplicationProperty SITE = new ApplicationProperty("site");
    public static final ApplicationProperty CODE = new ApplicationProperty("code");
    public static final ApplicationProperty ACTIVE = new ApplicationProperty("active");
    private ApplicationProperty(String name) { super(Application.class, name); }
}
