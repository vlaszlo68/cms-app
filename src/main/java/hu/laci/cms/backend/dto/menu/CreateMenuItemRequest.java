package hu.laci.cms.backend.dto.menu;

import hu.laci.cms.backend.model.menu.MenuItemTargetType;

public class CreateMenuItemRequest extends MenuItemRequestBase {

    public CreateMenuItemRequest() {
    }

    public CreateMenuItemRequest(Long menuId, Long parentId, Long pageId, String title, Integer sortOrder,
                                 Boolean visible) {
        super(menuId, parentId, pageId, title, sortOrder, visible);
    }

    public CreateMenuItemRequest(Long menuId, Long parentId, Long pageId, MenuItemTargetType targetType,
                                 String targetUrl, String title, Integer sortOrder, Boolean visible) {
        super(menuId, parentId, pageId, targetType, targetUrl, title, sortOrder, visible);
    }
}
