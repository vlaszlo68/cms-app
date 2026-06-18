package hu.laci.cms.backend.dto.menu;

import hu.laci.cms.backend.model.menu.MenuItemTargetType;

public class MenuItemResponse {

    private final Long id;
    private final Long menuId;
    private final Long parentId;
    private final Long pageId;
    private final MenuItemTargetType targetType;
    private final String targetUrl;
    private final String title;
    private final Integer sortOrder;
    private final boolean visible;

    public MenuItemResponse(Long id, Long menuId, Long parentId, Long pageId, String title, Integer sortOrder,
                            boolean visible) {
        this(id, menuId, parentId, pageId, MenuItemTargetType.PAGE, null, title, sortOrder, visible);
    }

    public MenuItemResponse(Long id, Long menuId, Long parentId, Long pageId, MenuItemTargetType targetType,
                            String targetUrl, String title, Integer sortOrder, boolean visible) {
        this.id = id;
        this.menuId = menuId;
        this.parentId = parentId;
        this.pageId = pageId;
        this.targetType = targetType;
        this.targetUrl = targetUrl;
        this.title = title;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    public Long getId() {
        return id;
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

    public boolean isVisible() {
        return visible;
    }
}
