package hu.laci.cms.backend.model.page;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

public final class PageProperty extends AuditableProperty {

    public static final PageProperty ID = new PageProperty(BaseProperty.ID.getPropertyName());
    public static final PageProperty TITLE = new PageProperty("title");
    public static final PageProperty SLUG = new PageProperty("slug");
    public static final PageProperty CONTENT = new PageProperty("content");
    public static final PageProperty PAGE_TYPE = new PageProperty("pageType");
    public static final PageProperty STATUS = new PageProperty("status");
    public static final PageProperty META_TITLE = new PageProperty("metaTitle");
    public static final PageProperty META_DESCRIPTION = new PageProperty("metaDescription");
    public static final PageProperty HOMEPAGE = new PageProperty("homepage");
    public static final PageProperty MENU_VISIBLE = new PageProperty("menuVisible");
    public static final PageProperty TEMPLATE_ID = new PageProperty("templateId");
    public static final PageProperty CREATED_AT = new PageProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final PageProperty UPDATED_AT = new PageProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final PageProperty CREATED_BY = new PageProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final PageProperty UPDATED_BY = new PageProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private PageProperty(String propertyName) {
        super(Page.class, propertyName);
    }
}
