package hu.laci.cms.backend.model.menu;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

public final class MenuProperty extends AuditableProperty {

    public static final MenuProperty ID = new MenuProperty(BaseProperty.ID.getPropertyName());
    public static final MenuProperty NAME = new MenuProperty("name");
    public static final MenuProperty CODE = new MenuProperty("code");
    public static final MenuProperty ACTIVE = new MenuProperty("active");
    public static final MenuProperty CREATED_AT = new MenuProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final MenuProperty UPDATED_AT = new MenuProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final MenuProperty CREATED_BY = new MenuProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final MenuProperty UPDATED_BY = new MenuProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private MenuProperty(String propertyName) {
        super(Menu.class, propertyName);
    }
}
