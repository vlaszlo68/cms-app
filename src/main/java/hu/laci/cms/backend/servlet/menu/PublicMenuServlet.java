package hu.laci.cms.backend.servlet.menu;

import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.service.menu.MenuItemService;
import hu.laci.cms.backend.service.menu.MenuService;
import hu.laci.cms.backend.service.menu.MenuServiceException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/api/public/menus/*")
public class PublicMenuServlet extends JsonServletSupport {

    private MenuItemService menuItemService;

    @Override
    public void init() throws ServletException {
        MenuDao menuDao = DaoRegistry.getDao(Menu.class);
        MenuItemDao menuItemDao = DaoRegistry.getDao(MenuItem.class);
        menuItemService = new MenuItemService(menuDao, menuItemDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    menuItemService.getPublicMenu(parseCode(request)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (MenuServiceException e) {
            int status = MenuService.MENU_NOT_FOUND.equals(e.getCode())
                    ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_BAD_REQUEST;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        }
    }

    private String parseCode(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path) || !path.startsWith("/")) {
            throw new BadRequestException("Menu code is required.");
        }
        String code = path.substring(1);
        if (code.isBlank() || code.contains("/")) {
            throw new BadRequestException("Invalid menu code path.");
        }
        return code;
    }

    private static final class BadRequestException extends RuntimeException {
        private BadRequestException(String message) {
            super(message);
        }
    }
}
