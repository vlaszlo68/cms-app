package hu.laci.cms.backend.servlet.site;

import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dao.application.SiteApplicationDao;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.auth.*;
import hu.laci.cms.backend.config.session.AppSession;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.service.AuthService;
import hu.laci.cms.backend.service.AuthServiceException;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.service.menu.MenuItemService;
import hu.laci.cms.backend.service.menu.MenuServiceException;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.page.PageServiceException;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.service.settings.SiteSettingsServiceException;
import hu.laci.cms.backend.service.site.SiteService;
import hu.laci.cms.backend.service.site.SiteServiceException;
import hu.laci.cms.backend.service.application.ApplicationDefinitionRegistry;
import hu.laci.cms.backend.service.application.ApplicationPlatformRegistry;
import hu.laci.cms.backend.service.application.SiteApplicationService;
import hu.laci.cms.backend.service.application.SiteApplicationServiceException;
import hu.laci.cms.backend.service.function.FunctionRegistry;
import hu.laci.cms.backend.service.function.PublicFunctionException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Anonymous dispatcher for published content belonging to one explicit active Site.
 */
@WebServlet(urlPatterns = "/api/public/sites/*")
public class PublicSiteServlet extends JsonServletSupport {

    private SiteService siteService;
    private PageService pageService;
    private MenuItemService menuItemService;
    private SiteSettingsService siteSettingsService;
    private SiteApplicationService applicationService;
    private PublicFunctionServlet publicFunctionServlet;
    private AuthService authService;

    @Override
    public void init() throws ServletException {
        SiteDao siteDao = DaoRegistry.getDao(Site.class);
        PageDao pageDao = DaoRegistry.getDao(Page.class);
        PageBlockDao pageBlockDao = DaoRegistry.getDao(PageBlock.class);
        TemplateDao templateDao = DaoRegistry.getDao(Template.class);
        MenuDao menuDao = DaoRegistry.getDao(Menu.class);
        MenuItemDao menuItemDao = DaoRegistry.getDao(MenuItem.class);
        SiteSettingsDao settingsDao = DaoRegistry.getDao(SiteSettings.class);
        MediaDao mediaDao = DaoRegistry.getDao(Media.class);
        SiteApplicationDao applicationDao = DaoRegistry.getDao(SiteApplication.class);
        UserDao userDao = DaoRegistry.getDao(User.class);
        ApplicationPlatformRegistry platformRegistry = ApplicationPlatformRegistry.production();
        ApplicationDefinitionRegistry applicationRegistry = platformRegistry.getApplicationDefinitions();
        siteService = new SiteService(siteDao);
        pageService = new PageService(pageDao, templateDao, pageBlockDao);
        menuItemService = new MenuItemService(menuDao, menuItemDao, pageDao, applicationDao, applicationRegistry);
        siteSettingsService = new SiteSettingsService(settingsDao, mediaDao);
        applicationService = new SiteApplicationService(applicationDao, applicationRegistry);
        publicFunctionServlet = new PublicFunctionServlet(siteService, applicationService,
                platformRegistry.getPublicFunctions());
        authService = new AuthService(userDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<String> path = pathSegments(request);
            if (path.size() < 2) {
                throw new BadRequestException("Site code and public resource are required.");
            }
            Site site = siteService.requireActiveSite(path.get(0));
            Object data;
            if (path.size() == 3 && "auth".equals(path.get(1)) && "me".equals(path.get(2))) {
                data = publicAuthState(request, response);
                writeJsonResponseIncludingNulls(response, HttpServletResponse.SC_OK, data);
                return;
            } else if (path.size() == 3 && "pages".equals(path.get(1))) {
                data = pageService.getPublicPageBySlug(site, path.get(2));
            } else if (path.size() == 3 && "menus".equals(path.get(1))) {
                data = menuItemService.getPublicMenu(site, path.get(2));
            } else if (path.size() == 3 && "applications".equals(path.get(1))) {
                data = applicationService.publicResponse(applicationService.requirePublic(site, path.get(2)));
            } else if (path.size() == 2 && "site-settings".equals(path.get(1))) {
                writeJsonResponseIncludingNulls(response, HttpServletResponse.SC_OK,
                        siteSettingsService.getPublicSettings(site));
                return;
            } else {
                throw new BadRequestException("Invalid public Site path.");
            }
            writeJsonResponse(response, HttpServletResponse.SC_OK, data);
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (SiteServiceException e) {
            int status = SiteService.SITE_NOT_FOUND.equals(e.getCode())
                    ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        } catch (PageServiceException e) {
            int status = PageService.PAGE_NOT_FOUND.equals(e.getCode())
                    ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        } catch (MenuServiceException e) {
            int status = MenuItemService.MENU_NOT_FOUND.equals(e.getCode())
                    ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        } catch (SiteSettingsServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getCode(), e.getMessage());
        } catch (SiteApplicationServiceException e) {
            int status = SiteApplicationService.APPLICATION_NOT_FOUND.equals(e.getCode()) ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_BAD_REQUEST;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<String> path = pathSegments(request);
            if (path.size() == 3 && "auth".equals(path.get(1)) && "login".equals(path.get(2))) {
                siteService.requireActiveSite(path.get(0));
                Object data = login(request, response);
                writeJsonResponse(response, HttpServletResponse.SC_OK, data);
                return;
            }
            if (path.size() == 3 && "auth".equals(path.get(1)) && "logout".equals(path.get(2))) {
                siteService.requireActiveSite(path.get(0));
                AppSessionManager.invalidate(request, response);
                writeJsonResponse(response, HttpServletResponse.SC_OK, java.util.Map.of("message", "Logged out"));
                return;
            }
            if (path.size() != 5 || !"applications".equals(path.get(1)) || !"functions".equals(path.get(3))) throw new BadRequestException("Invalid public function path.");
            Object data = publicFunctionServlet.dispatch(path.get(0), path.get(2), path.get(4), request.getReader(), request, response);
            writeJsonResponse(response, HttpServletResponse.SC_OK, data);
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (SiteServiceException | SiteApplicationServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e instanceof SiteServiceException ? ((SiteServiceException) e).getCode() : ((SiteApplicationServiceException) e).getCode(), "Resource not found.");
        } catch (PublicFunctionException e) {
            int status = "AUTH_REQUIRED".equals(e.getCode()) || "INVALID_CREDENTIALS".equals(e.getCode()) ? HttpServletResponse.SC_UNAUTHORIZED
                    : "FORBIDDEN".equals(e.getCode()) || "CSRF_INVALID".equals(e.getCode()) ? HttpServletResponse.SC_FORBIDDEN
                    : PublicFunctionServlet.INVALID_FUNCTION_INPUT.equals(e.getCode()) ? HttpServletResponse.SC_BAD_REQUEST
                    : PublicFunctionServlet.FUNCTION_NOT_FOUND.equals(e.getCode()) ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        } catch (AuthServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error.");
        }
    }

    private PublicAuthStateResponse login(HttpServletRequest request,HttpServletResponse response) throws IOException {
        PublicLoginRequest loginRequest;
        try { loginRequest = gson.fromJson(request.getReader(), PublicLoginRequest.class); }
        catch (RuntimeException e) { throw new BadRequestException("Invalid JSON request body."); }
        if (loginRequest == null || isBlank(loginRequest.getEmail()) || isBlank(loginRequest.getPassword())) throw new BadRequestException("email and password are required.");
        Optional<User> user = authService.loginByEmail(loginRequest.getEmail().trim(),loginRequest.getPassword(),request.getRemoteAddr());
        if (user.isEmpty()) throw new PublicFunctionException("INVALID_CREDENTIALS","Invalid credentials");
        User authenticated=user.get();
        AppSession session=AppSessionManager.createAuthenticatedSession(request,response,new AuthenticatedUser(authenticated.getId(),authenticated.getLoginName(),authenticated.getEmailAddress(),authenticated.getRole()));
        return PublicAuthStateResponse.authenticated(session.getAuthenticatedUser().orElseThrow(),AppSessionManager.ensureCsrfToken(request,response,session));
    }
    private PublicAuthStateResponse publicAuthState(HttpServletRequest request,HttpServletResponse response) {
        Optional<AppSession> session=AppSessionManager.findSession(request,response);
        if (session.isEmpty() || session.get().getAuthenticatedUser().isEmpty()) return PublicAuthStateResponse.anonymous();
        return PublicAuthStateResponse.authenticated(session.get().getAuthenticatedUser().orElseThrow(),AppSessionManager.ensureCsrfToken(request,response,session.get()));
    }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private List<String> pathSegments(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)
                || !pathInfo.startsWith("/") || pathInfo.endsWith("/") || pathInfo.contains("//")) {
            throw new BadRequestException("Invalid public Site path.");
        }
        List<String> segments = Arrays.asList(pathInfo.substring(1).split("/", -1));
        if (segments.stream().anyMatch(String::isBlank)) {
            throw new BadRequestException("Invalid public Site path.");
        }
        return segments;
    }

    private static final class BadRequestException extends RuntimeException {
        private BadRequestException(String message) { super(message); }
    }
}
