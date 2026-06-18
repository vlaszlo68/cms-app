package hu.laci.cms.backend.model.menu;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

public final class MenuItemProperty extends AuditableProperty {

    public static final MenuItemProperty ID = new MenuItemProperty(BaseProperty.ID.getPropertyName());
    public static final MenuItemProperty MENU_ID = new MenuItemProperty("menuId");
    public static final MenuItemProperty PARENT_ID = new MenuItemProperty("parentId");
    public static final MenuItemProperty PAGE_ID = new MenuItemProperty("pageId");
    public static final MenuItemProperty TARGET_TYPE = new MenuItemProperty("targetType");
    public static final MenuItemProperty TARGET_URL = new MenuItemProperty("targetUrl");
    public static final MenuItemProperty TITLE = new MenuItemProperty("title");
    public static final MenuItemProperty SORT_ORDER = new MenuItemProperty("sortOrder");
    public static final MenuItemProperty VISIBLE = new MenuItemProperty("visible");
    public static final MenuItemProperty CREATED_AT = new MenuItemProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final MenuItemProperty UPDATED_AT = new MenuItemProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final MenuItemProperty CREATED_BY = new MenuItemProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final MenuItemProperty UPDATED_BY = new MenuItemProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private MenuItemProperty(String propertyName) {
        super(MenuItem.class, propertyName);
    }
}
