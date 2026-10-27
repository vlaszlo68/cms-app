package hu.laci.cms.backend.service.menu;

import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.application.ApplicationDao;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.service.application.ApplicationDefinitionRegistry;
import hu.laci.cms.backend.dto.menu.CreateMenuItemRequest;
import hu.laci.cms.backend.dto.menu.MenuItemRequestBase;
import hu.laci.cms.backend.dto.menu.MenuItemResponse;
import hu.laci.cms.backend.dto.menu.PublicMenuItemResponse;
import hu.laci.cms.backend.dto.menu.UpdateMenuItemRequest;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.menu.MenuItemTargetType;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.site.Site;

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
    public static final String INVALID_PAGE_TARGET = "INVALID_PAGE_TARGET";
    public static final String INVALID_APPLICATION_TARGET = "INVALID_APPLICATION_TARGET";

    private final MenuDao menuDao;
    private final MenuItemDao menuItemDao;
    private final PageDao pageDao;
    private final ApplicationDao applicationDao;
    private final ApplicationDefinitionRegistry applicationRegistry;

    /**
     * Creates the menu-item service with the bounded public-page lookup used by anonymous navigation.
     *
     * @param menuDao menu persistence dependency
     * @param menuItemDao menu item persistence dependency
     * @param pageDao page persistence dependency for public PAGE target eligibility
     */
    public MenuItemService(MenuDao menuDao, MenuItemDao menuItemDao, PageDao pageDao) {
        this(menuDao, menuItemDao, pageDao, null, ApplicationDefinitionRegistry.empty());
    }

    public MenuItemService(MenuDao menuDao, MenuItemDao menuItemDao, PageDao pageDao,
                           ApplicationDao applicationDao, ApplicationDefinitionRegistry applicationRegistry) {
        this.menuDao = Objects.requireNonNull(menuDao, "menuDao must not be null");
        this.menuItemDao = Objects.requireNonNull(menuItemDao, "menuItemDao must not be null");
        this.pageDao = Objects.requireNonNull(pageDao, "pageDao must not be null");
        this.applicationDao = applicationDao;
        this.applicationRegistry = Objects.requireNonNull(applicationRegistry, "applicationRegistry must not be null");
    }

    public List<MenuItemResponse> listItems(Site site, Long menuId) {
        loadMenu(site, menuId);
        return menuItemDao.findByMenuId(menuId).stream().map(item -> toResponse(site, item)).toList();
    }

    public MenuItemResponse createItem(Site site, CreateMenuItemRequest request) {
        Long menuId = request == null ? null : request.getMenuId();
        return createItem(site, menuId, request);
    }

    public MenuItemResponse createItem(Site site, Long menuId, CreateMenuItemRequest request) {
        validate(request);
        requirePathMenu(request.getMenuId(), menuId);
        loadMenu(site, menuId);
        validateParent(request.getParentId(), menuId, null);
        Target target = normalizeTarget(site, request);
        MenuItem item = new MenuItem(null, menuId, request.getParentId(), target.pageId(),
                target.type(), target.url(), request.getTitle().trim(),
                defaultOrder(request.getSortOrder()),
                request.getVisible() == null || request.getVisible());
        item.setApplication(target.application());
        return toResponse(site, menuItemDao.create(item));
    }

    public MenuItemResponse updateItem(Site site, Long menuId, Long id, UpdateMenuItemRequest request) {
        validate(request);
        requirePathMenu(request.getMenuId(), menuId);
        loadMenu(site, menuId);
        MenuItem item = loadItem(menuId, id);
        validateParent(request.getParentId(), menuId, id);
        Target target = normalizeTarget(site, request);
        item.setMenuId(menuId);
        item.setParentId(request.getParentId());
        item.setPageId(target.pageId());
        item.setApplication(target.application());
        item.setTargetType(target.type());
        item.setTargetUrl(target.url());
        item.setTitle(request.getTitle().trim());
        item.setSortOrder(defaultOrder(request.getSortOrder()));
        item.setVisible(request.getVisible() == null || request.getVisible());
        return toResponse(site, menuItemDao.update(item));
    }

    public MenuItemResponse updateItem(Site site, Long id, UpdateMenuItemRequest request) {
        Long menuId = request == null ? null : request.getMenuId();
        return updateItem(site, menuId, id, request);
    }

    public boolean deleteItem(Site site, Long menuId, Long id) {
        loadMenu(site, menuId);
        return menuItemDao.delete(loadItem(menuId, id));
    }

    public boolean deleteItem(Site site, Long id) {
        if (id == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "Menu item id is required.");
        }
        MenuItem item = menuItemDao.findById(id)
                .orElseThrow(() -> new MenuServiceException(MENU_ITEM_NOT_FOUND, "Menu item not found."));
        loadMenu(site, item.getMenuId());
        return menuItemDao.delete(item);
    }

    public List<PublicMenuItemResponse> getPublicMenu(Site site, String code) {
        requireSite(site);
        if (code == null || code.isBlank()) {
            throw new MenuServiceException(MenuService.VALIDATION_ERROR, "code is required.");
        }
        Menu menu = menuDao.findBySiteAndCode(site, code.trim())
                .filter(Menu::isActive)
                .orElseThrow(() -> new MenuServiceException(MenuService.MENU_NOT_FOUND, "Menu not found."));

        List<MenuItem> items = menuItemDao.findByMenuId(menu.getId());
        Set<Long> pageIds = items.stream()
                .filter(item -> item.getTargetType() == MenuItemTargetType.PAGE)
                .map(MenuItem::getPageId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<Long, Page> publicPages = pageDao.findPublishedContentBySiteAndIds(site, pageIds).stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Page::getId, page -> page));
        Set<Long> applicationIds = items.stream().filter(item -> item.getTargetType() == MenuItemTargetType.APPLICATION)
                .map(MenuItem::getApplication).filter(Objects::nonNull).map(Application::getId).filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<Long, Application> publicApplications = applicationDao == null ? Map.of() : applicationDao.findAllBySite(site).stream()
                .filter(Application::isActive).filter(app -> applicationRegistry.isSupported(app.getCode()))
                .filter(app -> applicationIds.contains(app.getId())).collect(java.util.stream.Collectors.toUnmodifiableMap(Application::getId, app -> app));
        Map<Long, List<MenuItem>> children = new HashMap<>();
        for (MenuItem item : items) {
            children.computeIfAbsent(item.getParentId(), ignored -> new ArrayList<>()).add(item);
        }
        return buildPublicChildren(null, children, publicPages, publicApplications, new HashSet<>());
    }

    private List<PublicMenuItemResponse> buildPublicChildren(Long parentId, Map<Long, List<MenuItem>> children,
                                                              Map<Long, Page> publicPages, Map<Long, Application> publicApplications, Set<Long> path) {
        List<PublicMenuItemResponse> result = new ArrayList<>();
        for (MenuItem item : children.getOrDefault(parentId, List.of())) {
            if (!item.isVisible() || !path.add(item.getId())) {
                continue;
            }
            Page page = item.getTargetType() == MenuItemTargetType.PAGE
                    ? publicPages.get(item.getPageId())
                    : null;
            Application application = item.getTargetType() == MenuItemTargetType.APPLICATION ? item.getApplication() : null;
            Application publicApplication = application == null ? null : publicApplications.get(application.getId());
            if ((item.getTargetType() == MenuItemTargetType.PAGE && page == null)
                    || (item.getTargetType() == MenuItemTargetType.APPLICATION && publicApplication == null)) {
                path.remove(item.getId());
                continue;
            }
            List<PublicMenuItemResponse> publicChildren = buildPublicChildren(item.getId(), children, publicPages, publicApplications,
                    path);
            if (publicApplication != null) {
                result.add(new PublicMenuItemResponse(item.getId(), item.getTitle(), item.getTargetType(),
                        publicApplication.getCode(), "/app/" + publicApplication.getCode(), publicChildren));
            } else {
                result.add(new PublicMenuItemResponse(item.getId(), item.getTitle(), item.getTargetType(),
                        page == null ? null : page.getId(), page == null ? null : page.getSlug(),
                        page == null ? null : publicPath(page), item.getTargetUrl(), publicChildren));
            }
            path.remove(item.getId());
        }
        return List.copyOf(result);
    }

    private static String publicPath(Page page) {
        return Boolean.TRUE.equals(page.getHomepage()) || "home".equals(page.getSlug())
                ? "/"
                : "/" + page.getSlug();
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

    private Menu loadMenu(Site site, Long id) {
        requireSite(site);
        if (id == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "menuId is required.");
        }
        return menuDao.findBySiteAndId(site, id)
                .orElseThrow(() -> new MenuServiceException(MENU_NOT_FOUND, "Menu not found."));
    }

    private MenuItem loadItem(Long menuId, Long id) {
        if (id == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "Menu item id is required.");
        }
        MenuItem item = menuItemDao.findById(id)
                .orElseThrow(() -> new MenuServiceException(MENU_ITEM_NOT_FOUND, "Menu item not found."));
        if (!menuId.equals(item.getMenuId())) {
            throw new MenuServiceException(MENU_ITEM_NOT_FOUND, "Menu item not found.");
        }
        return item;
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
        if (targetType == MenuItemTargetType.APPLICATION && request.getApplicationId() == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "applicationId is required for APPLICATION target.");
        }
        if (targetType == MenuItemTargetType.PAGE
                && (trimToNull(request.getTargetUrl()) != null || request.getApplicationId() != null)) {
            throw new MenuServiceException(VALIDATION_ERROR, "PAGE target cannot include URL or Application values.");
        }
        if (targetType == MenuItemTargetType.URL
                && (request.getPageId() != null || request.getApplicationId() != null)) {
            throw new MenuServiceException(VALIDATION_ERROR, "URL target cannot include Page or Application values.");
        }
        if (targetType == MenuItemTargetType.APPLICATION
                && (request.getPageId() != null || trimToNull(request.getTargetUrl()) != null)) {
            throw new MenuServiceException(VALIDATION_ERROR, "APPLICATION target cannot include Page or URL values.");
        }
    }

    private MenuItemResponse toResponse(Site site, MenuItem item) {
        Application application = item.getApplication();
        if (application != null && application.getId() != null && applicationDao != null
                && (application.getCode() == null || application.getName() == null)) {
            application = applicationDao.findBySiteAndId(site, application.getId()).orElse(null);
        }
        return new MenuItemResponse(item.getId(), item.getMenuId(), item.getParentId(), item.getPageId(),
                application == null ? null : application.getId(), application == null ? null : application.getCode(),
                application == null ? null : application.getName(), item.getTargetType(), item.getTargetUrl(),
                item.getTitle(), item.getSortOrder(), item.isVisible());
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

    private Target normalizeTarget(Site site, MenuItemRequestBase request) {
        MenuItemTargetType targetType = defaultTargetType(request.getTargetType());
        return switch (targetType) {
            case PAGE -> {
                if (pageDao.findBySiteAndId(site, request.getPageId()).isEmpty()) {
                    throw new MenuServiceException(INVALID_PAGE_TARGET,
                            "Target page must belong to the requested Site.");
                }
                yield new Target(targetType, request.getPageId(), null, null);
            }
            case URL -> new Target(targetType, null, null, trimToNull(request.getTargetUrl()));
            case APPLICATION -> {
                if (applicationDao == null) {
                    throw new MenuServiceException(INVALID_APPLICATION_TARGET, "Application targets are unavailable.");
                }
                Application application = applicationDao.findBySiteAndId(site, request.getApplicationId())
                        .orElseThrow(() -> new MenuServiceException(INVALID_APPLICATION_TARGET,
                                "Target application must belong to the requested Site."));
                if (!application.isActive() || !applicationRegistry.isSupported(application.getCode())) {
                    throw new MenuServiceException(INVALID_APPLICATION_TARGET, "Target application is not active and supported.");
                }
                yield new Target(targetType, null, application, null);
            }
        };
    }

    private static void requirePathMenu(Long requestMenuId, Long pathMenuId) {
        if (pathMenuId == null || requestMenuId == null || !pathMenuId.equals(requestMenuId)) {
            throw new MenuServiceException(VALIDATION_ERROR, "menuId must match the menu path.");
        }
    }

    private static void requireSite(Site site) {
        if (site == null || site.getId() == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "Site is required.");
        }
    }

    private record Target(MenuItemTargetType type, Long pageId, Application application, String url) {
    }
}
