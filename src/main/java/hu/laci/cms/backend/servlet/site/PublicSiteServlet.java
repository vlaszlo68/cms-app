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
import hu.laci.cms.backend.dao.application.ApplicationDao;
import hu.laci.cms.backend.model.application.Application;
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
import hu.laci.cms.backend.service.application.ApplicationService;
import hu.laci.cms.backend.service.application.ApplicationServiceException;
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

/**
 * Anonymous dispatcher for published content belonging to one explicit active Site.
 */
@WebServlet(urlPatterns = "/api/public/sites/*")
public class PublicSiteServlet extends JsonServletSupport {

    private SiteService siteService;
    private PageService pageService;
    private MenuItemService menuItemService;
    private SiteSettingsService siteSettingsService;
    private ApplicationService applicationService;
    private PublicFunctionServlet publicFunctionServlet;

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
        ApplicationDao applicationDao = DaoRegistry.getDao(Application.class);
        ApplicationPlatformRegistry platformRegistry = ApplicationPlatformRegistry.production();
        ApplicationDefinitionRegistry applicationRegistry = platformRegistry.getApplicationDefinitions();
        siteService = new SiteService(siteDao);
        pageService = new PageService(pageDao, templateDao, pageBlockDao);
        menuItemService = new MenuItemService(menuDao, menuItemDao, pageDao, applicationDao, applicationRegistry);
        siteSettingsService = new SiteSettingsService(settingsDao, mediaDao);
        applicationService = new ApplicationService(applicationDao, applicationRegistry);
        publicFunctionServlet = new PublicFunctionServlet(siteService, applicationService,
                platformRegistry.getPublicFunctions());
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
            if (path.size() == 3 && "pages".equals(path.get(1))) {
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
        } catch (ApplicationServiceException e) {
            int status = ApplicationService.APPLICATION_NOT_FOUND.equals(e.getCode()) ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_BAD_REQUEST;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<String> path = pathSegments(request);
            if (path.size() != 5 || !"applications".equals(path.get(1)) || !"functions".equals(path.get(3))) {
                throw new BadRequestException("Invalid public function path.");
            }
            Object data = publicFunctionServlet.dispatch(path.get(0), path.get(2), path.get(4), request.getReader());
            writeJsonResponse(response, HttpServletResponse.SC_OK, data);
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (SiteServiceException | ApplicationServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, e instanceof SiteServiceException ? ((SiteServiceException) e).getCode() : ((ApplicationServiceException) e).getCode(), "Resource not found.");
        } catch (PublicFunctionException e) {
            int status = PublicFunctionServlet.INVALID_FUNCTION_INPUT.equals(e.getCode()) ? HttpServletResponse.SC_BAD_REQUEST
                    : PublicFunctionServlet.FUNCTION_NOT_FOUND.equals(e.getCode()) ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        }
    }

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
