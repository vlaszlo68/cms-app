package hu.laci.cms.backend.service.menu;

import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.dto.menu.CreateMenuItemRequest;
import hu.laci.cms.backend.dto.menu.MenuItemRequestBase;
import hu.laci.cms.backend.dto.menu.MenuItemResponse;
import hu.laci.cms.backend.dto.menu.PublicMenuItemResponse;
import hu.laci.cms.backend.dto.menu.UpdateMenuItemRequest;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.menu.MenuItemTargetType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MenuItemService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MENU_NOT_FOUND = "MENU_NOT_FOUND";
    public static final String MENU_ITEM_NOT_FOUND = "MENU_ITEM_NOT_FOUND";
    public static final String INVALID_PARENT = "INVALID_PARENT";

    private final MenuDao menuDao;
    private final MenuItemDao menuItemDao;

    public MenuItemService(MenuDao menuDao, MenuItemDao menuItemDao) {
        this.menuDao = Objects.requireNonNull(menuDao, "menuDao must not be null");
        this.menuItemDao = Objects.requireNonNull(menuItemDao, "menuItemDao must not be null");
    }

    public List<MenuItemResponse> listItems(Long menuId) {
        loadMenu(menuId);
        return menuItemDao.findByMenuId(menuId).stream().map(this::toResponse).toList();
    }

    public MenuItemResponse createItem(CreateMenuItemRequest request) {
        validate(request);
        loadMenu(request.getMenuId());
        validateParent(request.getParentId(), request.getMenuId(), null);
        Target target = normalizeTarget(request);
        MenuItem item = new MenuItem(null, request.getMenuId(), request.getParentId(), target.pageId(),
                target.type(), target.url(), request.getTitle().trim(),
                defaultOrder(request.getSortOrder()),
                request.getVisible() == null || request.getVisible());
        return toResponse(menuItemDao.create(item));
    }

    public MenuItemResponse updateItem(Long id, UpdateMenuItemRequest request) {
        validate(request);
        MenuItem item = loadItem(id);
        loadMenu(request.getMenuId());
        validateParent(request.getParentId(), request.getMenuId(), id);
        Target target = normalizeTarget(request);
        item.setMenuId(request.getMenuId());
        item.setParentId(request.getParentId());
        item.setPageId(target.pageId());
        item.setTargetType(target.type());
        item.setTargetUrl(target.url());
        item.setTitle(request.getTitle().trim());
        item.setSortOrder(defaultOrder(request.getSortOrder()));
        item.setVisible(request.getVisible() == null || request.getVisible());
        return toResponse(menuItemDao.update(item));
    }

    public boolean deleteItem(Long id) {
        return menuItemDao.delete(loadItem(id));
    }

    public List<PublicMenuItemResponse> getPublicMenu(String code) {
        if (code == null || code.isBlank()) {
            throw new MenuServiceException(MenuService.VALIDATION_ERROR, "code is required.");
        }
        Menu menu = menuDao.findByCode(code.trim())
                .filter(Menu::isActive)
                .orElseThrow(() -> new MenuServiceException(MenuService.MENU_NOT_FOUND, "Menu not found."));

        List<MenuItem> items = menuItemDao.findByMenuId(menu.getId());
        Map<Long, List<MenuItem>> children = new HashMap<>();
        for (MenuItem item : items) {
            children.computeIfAbsent(item.getParentId(), ignored -> new ArrayList<>()).add(item);
        }
        return buildPublicChildren(null, children, new HashSet<>());
    }

    private List<PublicMenuItemResponse> buildPublicChildren(Long parentId, Map<Long, List<MenuItem>> children,
                                                              Set<Long> path) {
        List<PublicMenuItemResponse> result = new ArrayList<>();
        for (MenuItem item : children.getOrDefault(parentId, List.of())) {
            if (!item.isVisible() || !path.add(item.getId())) {
                continue;
            }
            result.add(new PublicMenuItemResponse(item.getTitle(), item.getTargetType(), item.getPageId(),
                    item.getTargetUrl(),
                    buildPublicChildren(item.getId(), children, path)));
            path.remove(item.getId());
        }
        return List.copyOf(result);
    }

    private void validateParent(Long parentId, Long menuId, Long currentItemId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(currentItemId)) {
            throw new MenuServiceException(INVALID_PARENT, "A menu item cannot be its own parent.");
        }
        MenuItem parent = menuItemDao.findById(parentId)
                .orElseThrow(() -> new MenuServiceException(INVALID_PARENT, "Parent menu item not found."));
        if (!parent.getMenuId().equals(menuId)) {
            throw new MenuServiceException(INVALID_PARENT, "Parent menu item belongs to another menu.");
        }
        Long ancestorId = parent.getParentId();
        Set<Long> visited = new HashSet<>();
        while (ancestorId != null && visited.add(ancestorId)) {
            if (ancestorId.equals(currentItemId)) {
                throw new MenuServiceException(INVALID_PARENT, "Menu item hierarchy cannot contain a cycle.");
            }
            ancestorId = menuItemDao.findById(ancestorId).map(MenuItem::getParentId).orElse(null);
        }
    }

    private Menu loadMenu(Long id) {
        if (id == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "menuId is required.");
        }
        return menuDao.findById(id)
                .orElseThrow(() -> new MenuServiceException(MENU_NOT_FOUND, "Menu not found."));
    }

    private MenuItem loadItem(Long id) {
        if (id == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "Menu item id is required.");
        }
        return menuItemDao.findById(id)
                .orElseThrow(() -> new MenuServiceException(MENU_ITEM_NOT_FOUND, "Menu item not found."));
    }

    private static void validate(MenuItemRequestBase request) {
        if (request == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "Request body is required.");
        }
        if (request.getMenuId() == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "menuId is required.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new MenuServiceException(VALIDATION_ERROR, "title is required.");
        }
        MenuItemTargetType targetType = defaultTargetType(request.getTargetType());
        if (targetType == MenuItemTargetType.PAGE && request.getPageId() == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "pageId is required for PAGE target.");
        }
        if (targetType == MenuItemTargetType.URL && trimToNull(request.getTargetUrl()) == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "targetUrl is required for URL target.");
        }
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getMenuId(), item.getParentId(), item.getPageId(),
                item.getTargetType(), item.getTargetUrl(), item.getTitle(), item.getSortOrder(), item.isVisible());
    }

    private static int defaultOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private static MenuItemTargetType defaultTargetType(MenuItemTargetType targetType) {
        return targetType == null ? MenuItemTargetType.PAGE : targetType;
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static Target normalizeTarget(MenuItemRequestBase request) {
        MenuItemTargetType targetType = defaultTargetType(request.getTargetType());
        return switch (targetType) {
            case PAGE -> new Target(targetType, request.getPageId(), null);
            case URL -> new Target(targetType, null, trimToNull(request.getTargetUrl()));
        };
    }

    private record Target(MenuItemTargetType type, Long pageId, String url) {
    }
}
