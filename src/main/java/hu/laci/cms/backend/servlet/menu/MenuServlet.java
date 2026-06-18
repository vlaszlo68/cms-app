package hu.laci.cms.backend.servlet.menu;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.menu.CreateMenuRequest;
import hu.laci.cms.backend.dto.menu.UpdateMenuRequest;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.menu.MenuItemService;
import hu.laci.cms.backend.service.menu.MenuService;
import hu.laci.cms.backend.service.menu.MenuServiceException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

@WebServlet(urlPatterns = {"/api/menus", "/api/menus/*"})
public class MenuServlet extends JsonServletSupport {

    private MenuService menuService;
    private MenuItemService menuItemService;

    @Override
    public void init() throws ServletException {
        MenuDao menuDao = DaoRegistry.getDao(Menu.class);
        MenuItemDao menuItemDao = DaoRegistry.getDao(MenuItem.class);
        menuService = new MenuService(menuDao);
        menuItemService = new MenuItemService(menuDao, menuItemDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            String path = path(request);
            if (path == null) {
                writeJsonResponse(response, HttpServletResponse.SC_OK, menuService.listMenus());
                return;
            }
            if (path.endsWith("/items")) {
                String idPart = path.substring(0, path.length() - "/items".length());
                writeJsonResponse(response, HttpServletResponse.SC_OK,
                        menuItemService.listItems(parseId(idPart, "Menu id")));
                return;
            }
            writeJsonResponse(response, HttpServletResponse.SC_OK, menuService.getMenu(parseId(path, "Menu id")));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (MenuServiceException e) {
            writeServiceError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            requireCollection(request);
            writeJsonResponse(response, HttpServletResponse.SC_CREATED,
                    menuService.createMenu(parseJson(request, CreateMenuRequest.class)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (MenuServiceException e) {
            writeServiceError(response, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            Long id = parseRequiredId(request);
            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    menuService.updateMenu(id, parseJson(request, UpdateMenuRequest.class)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (MenuServiceException e) {
            writeServiceError(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    menuService.deleteMenu(parseRequiredId(request)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (MenuServiceException e) {
            writeServiceError(response, e);
        }
    }

    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<AuthenticatedUser> user = AppSessionManager.getAuthenticatedUser(request, response);
        if (user.isEmpty()) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_REQUIRED", "Authentication required");
            return false;
        }
        if (user.get().getRole() != UserRole.ADMIN) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    "FORBIDDEN", "Administrator role is required.");
            return false;
        }
        return true;
    }

    private <T> T parseJson(HttpServletRequest request, Class<T> type) {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, type);
        } catch (IOException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid JSON request body.", e);
        }
    }

    private void requireCollection(HttpServletRequest request) {
        if (path(request) != null) {
            throw new BadRequestException("Endpoint does not accept a menu id.");
        }
    }

    private Long parseRequiredId(HttpServletRequest request) {
        String path = path(request);
        if (path == null) {
            throw new BadRequestException("Menu id is required.");
        }
        return parseId(path, "Menu id");
    }

    private String path(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return null;
        }
        if (!pathInfo.startsWith("/")) {
            throw new BadRequestException("Invalid menu path.");
        }
        return pathInfo.substring(1);
    }

    private Long parseId(String value, String label) {
        if (value.isBlank() || value.contains("/")) {
            throw new BadRequestException("Invalid menu path.");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException(label + " must be a number.", e);
        }
    }

    private void writeServiceError(HttpServletResponse response, MenuServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case MenuService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case MenuService.MENU_NOT_FOUND, MenuItemService.MENU_ITEM_NOT_FOUND ->
                    HttpServletResponse.SC_NOT_FOUND;
            case MenuService.DUPLICATE_CODE -> HttpServletResponse.SC_CONFLICT;
            case MenuItemService.INVALID_PARENT -> HttpServletResponse.SC_BAD_REQUEST;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        writeErrorResponse(response, status, e.getCode(), e.getMessage());
    }

    private static final class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }

        private BadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
