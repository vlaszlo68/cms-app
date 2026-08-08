package hu.laci.cms.backend.dto.menu;

import hu.laci.cms.backend.model.menu.MenuItemTargetType;

import java.util.List;

public class PublicMenuItemResponse {

    private final Long id;
    private final String title;
    private final MenuItemTargetType targetType;
    private final Long pageId;
    private final String pageSlug;
    private final String path;
    private final String targetUrl;
    private final List<PublicMenuItemResponse> children;

    public PublicMenuItemResponse(String title, List<PublicMenuItemResponse> children) {
        this(null, title, MenuItemTargetType.PAGE, null, null, null, null, children);
    }

    /**
     * Creates one anonymous public navigation item.
     *
     * @param id menu item id
     * @param title navigation label
     * @param targetType page or external URL target type
     * @param pageId eligible public page id for PAGE targets
     * @param pageSlug eligible public page slug for PAGE targets
     * @param path public route for PAGE targets
     * @param targetUrl external URL for URL targets
     * @param children ordered visible child items
     */
    public PublicMenuItemResponse(Long id, String title, MenuItemTargetType targetType, Long pageId,
                                  String pageSlug, String path, String targetUrl,
                                  List<PublicMenuItemResponse> children) {
        this.id = id;
        this.title = title;
        this.targetType = targetType;
        this.pageId = pageId;
        this.pageSlug = pageSlug;
        this.path = path;
        this.targetUrl = targetUrl;
        this.children = List.copyOf(children);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public MenuItemTargetType getTargetType() {
        return targetType;
    }

    public Long getPageId() {
        return pageId;
    }

    public String getPageSlug() {
        return pageSlug;
    }

    public String getPath() {
        return path;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public List<PublicMenuItemResponse> getChildren() {
        return children;
    }
}
