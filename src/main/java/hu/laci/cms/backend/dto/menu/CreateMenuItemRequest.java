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

    /**
     * Creates an item request for a PAGE, URL, or APPLICATION target.
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
    public CreateMenuItemRequest(Long menuId, Long parentId, Long pageId, Long applicationId,
                                 MenuItemTargetType targetType, String targetUrl, String title,
                                 Integer sortOrder, Boolean visible) {
        super(menuId, parentId, pageId, applicationId, targetType, targetUrl, title, sortOrder, visible);
    }
}
