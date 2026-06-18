package hu.laci.cms.backend.dto.menu;

import hu.laci.cms.backend.model.menu.MenuItemTargetType;

public abstract class MenuItemRequestBase {

    private Long menuId;
    private Long parentId;
    private Long pageId;
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
        this.menuId = menuId;
        this.parentId = parentId;
        this.pageId = pageId;
        this.targetType = targetType;
        this.targetUrl = targetUrl;
        this.title = title;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    public Long getMenuId() {
        return menuId;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getPageId() {
        return pageId;
    }

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
