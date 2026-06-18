package hu.laci.cms.backend.model.menu;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

@DbTable("menu_items")
public class MenuItem extends AuditableEntity {

    @DbColumn("menu_id")
    private Long menuId;

    @DbColumn("parent_id")
    private Long parentId;

    @DbColumn("page_id")
    private Long pageId;

    @DbColumn("target_type")
    private MenuItemTargetType targetType = MenuItemTargetType.PAGE;

    @DbColumn("target_url")
    private String targetUrl;

    @DbColumn("title")
    private String title;

    @DbColumn("sort_order")
    private Integer sortOrder = 0;

    @DbColumn("visible")
    private boolean visible = true;

    public MenuItem() {
    }

    public MenuItem(Long id, Long menuId, Long parentId, Long pageId, String title, Integer sortOrder,
                    boolean visible) {
        this(id, menuId, parentId, pageId, MenuItemTargetType.PAGE, null, title, sortOrder, visible);
    }

    public MenuItem(Long id, Long menuId, Long parentId, Long pageId, MenuItemTargetType targetType,
                    String targetUrl, String title, Integer sortOrder, boolean visible) {
        setId(id);
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

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public MenuItemTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(MenuItemTargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
