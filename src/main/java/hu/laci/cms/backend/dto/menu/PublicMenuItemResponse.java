package hu.laci.cms.backend.dto.menu;

import hu.laci.cms.backend.model.menu.MenuItemTargetType;

import java.util.List;

public class PublicMenuItemResponse {

    private final String title;
    private final MenuItemTargetType targetType;
    private final Long pageId;
    private final String targetUrl;
    private final List<PublicMenuItemResponse> children;

    public PublicMenuItemResponse(String title, List<PublicMenuItemResponse> children) {
        this(title, MenuItemTargetType.PAGE, null, null, children);
    }

    public PublicMenuItemResponse(String title, MenuItemTargetType targetType, Long pageId, String targetUrl,
                                  List<PublicMenuItemResponse> children) {
        this.title = title;
        this.targetType = targetType;
        this.pageId = pageId;
        this.targetUrl = targetUrl;
        this.children = List.copyOf(children);
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

    public String getTargetUrl() {
        return targetUrl;
    }

    public List<PublicMenuItemResponse> getChildren() {
        return children;
    }
}
