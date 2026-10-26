package hu.laci.cms.backend.service.menu;

import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dto.menu.CreateMenuRequest;
import hu.laci.cms.backend.dto.menu.MenuResponse;
import hu.laci.cms.backend.dto.menu.UpdateMenuRequest;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.site.Site;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MenuService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MENU_NOT_FOUND = "MENU_NOT_FOUND";
    public static final String DUPLICATE_CODE = "DUPLICATE_CODE";

    private final MenuDao menuDao;

    public MenuService(MenuDao menuDao) {
        this.menuDao = Objects.requireNonNull(menuDao, "menuDao must not be null");
    }

    public List<MenuResponse> listMenus(Site site) {
        requireSite(site);
        return menuDao.findAllBySite(site)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MenuResponse getMenu(Site site, Long id) {
        return toResponse(loadMenu(site, id));
    }

    public MenuResponse findByCode(Site site, String code) {
        requireSite(site);
        if (isBlank(code)) {
            throw new MenuServiceException(VALIDATION_ERROR, "code is required.");
        }
        return menuDao.findBySiteAndCode(site, code.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new MenuServiceException(MENU_NOT_FOUND, "Menu not found."));
    }

    public MenuResponse createMenu(Site site, CreateMenuRequest request) {
        requireSite(site);
        validate(request == null ? null : request.getName(), request == null ? null : request.getCode());
        String code = request.getCode().trim();
        ensureCodeAvailable(site, code, null);
        Menu menu = new Menu(null, request.getName().trim(), code,
                request.getActive() == null || request.getActive());
        menu.setSite(site);
        return toResponse(menuDao.create(menu));
    }

    public MenuResponse updateMenu(Site site, Long id, UpdateMenuRequest request) {
        requireSite(site);
        validate(request == null ? null : request.getName(), request == null ? null : request.getCode());
        Menu menu = loadMenu(site, id);
        String code = request.getCode().trim();
        ensureCodeAvailable(site, code, id);
        menu.setName(request.getName().trim());
        menu.setCode(code);
        menu.setActive(request.getActive() == null || request.getActive());
        return toResponse(menuDao.update(menu));
    }

    public boolean deleteMenu(Site site, Long id) {
        return menuDao.delete(loadMenu(site, id));
    }

    private Menu loadMenu(Site site, Long id) {
        requireSite(site);
        if (id == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "Menu id is required.");
        }
        return menuDao.findBySiteAndId(site, id)
                .orElseThrow(() -> new MenuServiceException(MENU_NOT_FOUND, "Menu not found."));
    }

    private void ensureCodeAvailable(Site site, String code, Long currentId) {
        Optional<Menu> existing = menuDao.findBySiteAndCode(site, code);
        if (existing.isPresent() && !existing.get().getId().equals(currentId)) {
            throw new MenuServiceException(DUPLICATE_CODE, "code is already used.");
        }
    }

    private static void validate(String name, String code) {
        if (isBlank(name)) {
            throw new MenuServiceException(VALIDATION_ERROR, "name is required.");
        }
        if (isBlank(code)) {
            throw new MenuServiceException(VALIDATION_ERROR, "code is required.");
        }
    }

    private MenuResponse toResponse(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getName(), menu.getCode(), menu.isActive());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void requireSite(Site site) {
        if (site == null || site.getId() == null) {
            throw new MenuServiceException(VALIDATION_ERROR, "Site is required.");
        }
    }
}
