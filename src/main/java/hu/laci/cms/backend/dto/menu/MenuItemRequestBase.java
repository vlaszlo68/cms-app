package hu.laci.cms.backend.dto.menu;

import hu.laci.cms.backend.model.menu.MenuItemTargetType;

public abstract class MenuItemRequestBase {

    private Long menuId;
    private Long parentId;
    private Long pageId;
    private Long applicationId;
    private MenuItemTargetType targetType;
    private String targetUrl;
    private String title;
    private Integer sortOrder;
    private Boolean visible;

    protected MenuItemRequestBase() {
    }

    protected MenuItemRequestBase(Long menuId, Long parentId, Long pageId, String title, Integer sortOrder,
                                  Boolean visible) {
        this(menuId, parentId, pageId, MenuItemTargetType.PAGE, null, title, sortOrder, visible);
    }

    protected MenuItemRequestBase(Long menuId, Long parentId, Long pageId, MenuItemTargetType targetType,
                                  String targetUrl, String title, Integer sortOrder, Boolean visible) {
        this(menuId, parentId, pageId, null, targetType, targetUrl, title, sortOrder, visible);
    }

    /**
     * Creates a menu-item request with an explicit target relationship.
     *
     * @param menuId owning menu id
     * @param parentId optional parent item id
     * @param pageId PAGE target id
     * @param applicationId APPLICATION target id
     * @param targetType target category
     * @param targetUrl URL target value
     * @param title item title
     * @param sortOrder display order
     * @param visible visibility flag
     */
    protected MenuItemRequestBase(Long menuId, Long parentId, Long pageId, Long applicationId,
                                  MenuItemTargetType targetType, String targetUrl, String title,
                                  Integer sortOrder, Boolean visible) {
        this.menuId = menuId;
        this.parentId = parentId;
        this.pageId = pageId;
        this.applicationId = applicationId;
        this.targetType = targetType;
        this.targetUrl = targetUrl;
        this.title = title;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    public Long getMenuId() {
        return menuId;
    }

    /**
     * Sets the owning menu id when normalizing a nested administration route.
     *
     * @param menuId owning menu id
     */
    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getPageId() {
        return pageId;
    }

    public Long getApplicationId() { return applicationId; }

    public MenuItemTargetType getTargetType() {
        return targetType;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getTitle() {
        return title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Boolean getVisible() {
        return visible;
    }
}
