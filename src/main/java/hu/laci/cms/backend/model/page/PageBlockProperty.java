package hu.laci.cms.backend.model.page;

import hu.laci.cms.backend.model.common.AuditableProperty;
import hu.laci.cms.backend.model.common.BaseProperty;

/**
 * Query properties available for {@link PageBlock} persistence operations.
 */
public final class PageBlockProperty extends AuditableProperty {

    public static final PageBlockProperty ID = new PageBlockProperty(BaseProperty.ID.getPropertyName());
    public static final PageBlockProperty PAGE_ID = new PageBlockProperty("pageId");
    public static final PageBlockProperty BLOCK_TYPE = new PageBlockProperty("blockType");
    public static final PageBlockProperty TITLE = new PageBlockProperty("title");
    public static final PageBlockProperty SORT_ORDER = new PageBlockProperty("sortOrder");
    public static final PageBlockProperty VISIBLE = new PageBlockProperty("visible");
    public static final PageBlockProperty CONFIG_JSON = new PageBlockProperty("configJson");
    public static final PageBlockProperty CREATED_AT = new PageBlockProperty(AuditableProperty.CREATED_AT.getPropertyName());
    public static final PageBlockProperty UPDATED_AT = new PageBlockProperty(AuditableProperty.UPDATED_AT.getPropertyName());
    public static final PageBlockProperty CREATED_BY = new PageBlockProperty(AuditableProperty.CREATED_BY.getPropertyName());
    public static final PageBlockProperty UPDATED_BY = new PageBlockProperty(AuditableProperty.UPDATED_BY.getPropertyName());

    private PageBlockProperty(String propertyName) {
        super(PageBlock.class, propertyName);
    }
}
