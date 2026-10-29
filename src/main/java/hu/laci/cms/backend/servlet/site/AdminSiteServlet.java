package hu.laci.cms.backend.servlet.site;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.config.media.MediaStorageConfig;
import hu.laci.cms.backend.config.media.MediaStorageServiceFactory;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dao.application.ApplicationDao;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.service.application.ApplicationDefinitionRegistry;
import hu.laci.cms.backend.service.application.ApplicationPlatformRegistry;
import hu.laci.cms.backend.service.application.ApplicationService;
import hu.laci.cms.backend.service.application.ApplicationServiceException;
import hu.laci.cms.backend.dto.application.CreateApplicationRequest;
import hu.laci.cms.backend.dto.application.UpdateApplicationRequest;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.menu.CreateMenuItemRequest;
import hu.laci.cms.backend.dto.menu.CreateMenuRequest;
import hu.laci.cms.backend.dto.menu.UpdateMenuItemRequest;
import hu.laci.cms.backend.dto.menu.UpdateMenuRequest;
import hu.laci.cms.backend.dto.page.CreatePageBlockRequest;
import hu.laci.cms.backend.dto.page.CreatePageRequest;
import hu.laci.cms.backend.dto.page.PageResponse;
import hu.laci.cms.backend.dto.page.PageWithBlocksResponse;
import hu.laci.cms.backend.dto.page.UpdatePageBlockRequest;
import hu.laci.cms.backend.dto.page.UpdatePageRequest;
import hu.laci.cms.backend.dto.settings.SaveSiteSettingsRequest;
import hu.laci.cms.backend.dto.site.CreateSiteRequest;
import hu.laci.cms.backend.dto.site.UpdateSiteRequest;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.media.MediaContent;
import hu.laci.cms.backend.service.media.MediaService;
import hu.laci.cms.backend.service.media.MediaServiceException;
import hu.laci.cms.backend.service.media.MediaStorageService;
import hu.laci.cms.backend.service.menu.MenuItemService;
import hu.laci.cms.backend.service.menu.MenuService;
import hu.laci.cms.backend.service.menu.MenuServiceException;
import hu.laci.cms.backend.service.page.PageBlockService;
import hu.laci.cms.backend.service.page.PageBlockServiceException;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.page.PageServiceException;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.service.settings.SiteSettingsServiceException;
import hu.laci.cms.backend.service.site.SiteService;
import hu.laci.cms.backend.service.site.SiteServiceException;
import hu.laci.cms.backend.service.site.SiteUsageService;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * Authenticated administrator dispatcher for Site management and all Site-owned resources.
 * Site context is always explicit in the URL and is never stored in the session.
 */
@MultipartConfig
@WebServlet(urlPatterns = {"/api/admin/sites", "/api/admin/sites/*"})
public class AdminSiteServlet extends JsonServletSupport {

    private SiteService siteService;
    private SiteUsageService siteUsageService;
    private PageService pageService;
    private PageBlockService pageBlockService;
    private MenuService menuService;
    private MenuItemService menuItemService;
    private SiteSettingsService siteSettingsService;
    private MediaService mediaService;
    private ApplicationService applicationService;

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
        MediaStorageConfig storageConfig = MediaStorageConfig.getCurrent();
        MediaStorageService storageService = MediaStorageServiceFactory.create(storageConfig);

        siteService = new SiteService(siteDao);
        siteUsageService = new SiteUsageService(siteDao, pageDao, menuDao, mediaDao, applicationDao, settingsDao);
        pageService = new PageService(pageDao, templateDao, pageBlockDao);
        pageBlockService = new PageBlockService(pageDao, pageBlockDao);
        menuService = new MenuService(menuDao);
        ApplicationDefinitionRegistry applicationRegistry = ApplicationPlatformRegistry.production()
                .getApplicationDefinitions();
        menuItemService = new MenuItemService(menuDao, menuItemDao, pageDao, applicationDao, applicationRegistry);
        applicationService = new ApplicationService(applicationDao, applicationRegistry);
        siteSettingsService = new SiteSettingsService(settingsDao, mediaDao);
        mediaService = new MediaService(mediaDao, storageService, storageConfig.getStorageType());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        dispatch(request, response, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        dispatch(request, response, "POST");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        dispatch(request, response, "PUT");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        dispatch(request, response, "DELETE");
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response, String method)
            throws IOException, ServletException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            ResponsePayload payload = route(request, method);
            if (payload.data() instanceof MediaContent mediaContent) {
                writeMediaContent(response, mediaContent);
                return;
            }
            writeJsonResponse(response, payload.status(), payload.data());
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (SiteServiceException e) {
            writeSiteError(response, e);
        } catch (PageServiceException e) {
            writePageError(response, e);
        } catch (PageBlockServiceException e) {
            writeBlockError(response, e);
        } catch (MenuServiceException e) {
            writeMenuError(response, e);
        } catch (SiteSettingsServiceException e) {
            writeSettingsError(response, e);
        } catch (MediaServiceException e) {
            writeMediaError(response, e);
        } catch (ApplicationServiceException e) {
            writeApplicationError(response, e);
        }
    }

    private ResponsePayload route(HttpServletRequest request, String method) throws IOException, ServletException {
        List<String> segments = pathSegments(request);
        if (segments.isEmpty()) {
            return switch (method) {
                case "GET" -> ok(siteService.listSites());
                case "POST" -> created(siteService.createSite(parseJson(request, CreateSiteRequest.class)));
                default -> throw unsupported();
            };
        }

        String siteCode = segments.get(0);
        if (segments.size() == 1) {
            return switch (method) {
                case "GET" -> ok(siteService.getSite(siteCode));
                case "PUT" -> ok(siteService.updateSite(siteCode, parseJson(request, UpdateSiteRequest.class)));
                case "DELETE" -> deleteSite(siteCode);
                default -> throw unsupported();
            };
        }

        if (segments.size() == 2 && "usage".equals(segments.get(1))) {
            if (!"GET".equals(method)) {
                throw unsupported();
            }
            return ok(siteUsageService.getUsage(siteService.requireSite(siteCode)));
        }

        Site site = siteService.requireSite(siteCode);
        String resource = segments.get(1);
        return switch (resource) {
            case "pages" -> routePages(request, method, site, segments.subList(2, segments.size()));
            case "menus" -> routeMenus(request, method, site, segments.subList(2, segments.size()));
            case "site-settings" -> routeSettings(request, method, site, segments.subList(2, segments.size()));
            case "media" -> routeMedia(request, method, site, segments.subList(2, segments.size()));
            case "applications" -> routeApplications(request, method, site, segments.subList(2, segments.size()));
            default -> throw new BadRequestException("Unknown Site resource path.");
        };
    }

    private ResponsePayload deleteSite(String siteCode) {
        Site site = siteService.requireSite(siteCode);
        siteUsageService.deleteSite(site);
        return ok(Map.of("message", "Site deleted."));
    }

    private ResponsePayload routeApplications(HttpServletRequest request, String method, Site site, List<String> path)
            throws IOException {
        if (path.isEmpty()) {
            return switch (method) {
                case "GET" -> ok(applicationService.list(site));
                case "POST" -> created(applicationService.create(site, parseJson(request, CreateApplicationRequest.class)));
                default -> throw unsupported();
            };
        }
        if (path.size() != 1) { throw new BadRequestException("Invalid application path."); }
        Long applicationId = parseId(path.get(0), "Application id");
        return switch (method) {
            case "GET" -> ok(applicationService.get(site, applicationId));
            case "PUT" -> ok(applicationService.update(site, applicationId, parseJson(request, UpdateApplicationRequest.class)));
            default -> throw unsupported();
        };
    }

    private ResponsePayload routePages(HttpServletRequest request, String method, Site site, List<String> path)
            throws IOException {
        if (path.isEmpty()) {
            return switch (method) {
                case "GET" -> ok(pageService.listPages(site));
                case "POST" -> created(pageService.createPage(site, parseJson(request, CreatePageRequest.class)));
                default -> throw unsupported();
            };
        }
        if (path.size() == 2 && "slug".equals(path.get(0)) && "GET".equals(method)) {
            return ok(pageService.getBySlug(site, path.get(1)));
        }

        Long pageId = parseId(path.get(0), "Page id");
        if (path.size() == 1) {
            return switch (method) {
                case "GET" -> {
                    PageResponse page = pageService.getPage(site, pageId);
                    if ("true".equalsIgnoreCase(request.getParameter("includeBlocks"))) {
                        yield ok(new PageWithBlocksResponse(page, pageBlockService.listBlocks(site, pageId)));
                    }
                    yield ok(page);
                }
                case "PUT" -> ok(pageService.updatePage(site, pageId,
                        parseJson(request, UpdatePageRequest.class)));
                case "DELETE" -> ok(pageService.deletePage(site, pageId));
                default -> throw unsupported();
            };
        }
        if (!"blocks".equals(path.get(1))) {
            throw new BadRequestException("Invalid page path.");
        }
        if (path.size() == 2) {
            return switch (method) {
                case "GET" -> ok(pageBlockService.listBlocks(site, pageId));
                case "POST" -> {
                    CreatePageBlockRequest body = parseJson(request, CreatePageBlockRequest.class);
                    if (body != null && body.getPageId() == null) {
                        body.setPageId(pageId);
                    }
                    yield created(pageBlockService.createBlock(site, pageId, body));
                }
                default -> throw unsupported();
            };
        }
        if (path.size() != 3) {
            throw new BadRequestException("Invalid page block path.");
        }
        Long blockId = parseId(path.get(2), "Page block id");
        return switch (method) {
            case "GET" -> ok(pageBlockService.getBlock(site, pageId, blockId));
            case "PUT" -> {
                UpdatePageBlockRequest body = parseJson(request, UpdatePageBlockRequest.class);
                if (body != null && body.getPageId() == null) {
                    body.setPageId(pageId);
                }
                yield ok(pageBlockService.updateBlock(site, pageId, blockId, body));
            }
            case "DELETE" -> ok(pageBlockService.deleteBlock(site, pageId, blockId));
            default -> throw unsupported();
        };
    }

    private ResponsePayload routeMenus(HttpServletRequest request, String method, Site site, List<String> path)
            throws IOException {
        if (path.isEmpty()) {
            return switch (method) {
                case "GET" -> ok(menuService.listMenus(site));
                case "POST" -> created(menuService.createMenu(site, parseJson(request, CreateMenuRequest.class)));
                default -> throw unsupported();
            };
        }
        Long menuId = parseId(path.get(0), "Menu id");
        if (path.size() == 1) {
            return switch (method) {
                case "GET" -> ok(menuService.getMenu(site, menuId));
                case "PUT" -> ok(menuService.updateMenu(site, menuId,
                        parseJson(request, UpdateMenuRequest.class)));
                case "DELETE" -> ok(menuService.deleteMenu(site, menuId));
                default -> throw unsupported();
            };
        }
        if (!"items".equals(path.get(1))) {
            throw new BadRequestException("Invalid menu path.");
        }
        if (path.size() == 2) {
            return switch (method) {
                case "GET" -> ok(menuItemService.listItems(site, menuId));
                case "POST" -> {
                    CreateMenuItemRequest body = parseJson(request, CreateMenuItemRequest.class);
                    if (body != null && body.getMenuId() == null) {
                        body.setMenuId(menuId);
                    }
                    yield created(menuItemService.createItem(site, menuId, body));
                }
                default -> throw unsupported();
            };
        }
        if (path.size() != 3) {
            throw new BadRequestException("Invalid menu item path.");
        }
        Long itemId = parseId(path.get(2), "Menu item id");
        return switch (method) {
            case "PUT" -> {
                UpdateMenuItemRequest body = parseJson(request, UpdateMenuItemRequest.class);
                if (body != null && body.getMenuId() == null) {
                    body.setMenuId(menuId);
                }
                yield ok(menuItemService.updateItem(site, menuId, itemId, body));
            }
            case "DELETE" -> ok(menuItemService.deleteItem(site, menuId, itemId));
            default -> throw unsupported();
        };
    }

    private ResponsePayload routeSettings(HttpServletRequest request, String method, Site site, List<String> path)
            throws IOException {
        if (!path.isEmpty()) {
            throw new BadRequestException("Invalid site-settings path.");
        }
        return switch (method) {
            case "GET" -> ok(siteSettingsService.getSettings(site));
            case "PUT" -> ok(siteSettingsService.saveSettings(site,
                    parseJson(request, SaveSiteSettingsRequest.class)));
            default -> throw unsupported();
        };
    }

    private ResponsePayload routeMedia(HttpServletRequest request, String method, Site site, List<String> path)
            throws IOException, ServletException {
        if (path.isEmpty()) {
            if ("GET".equals(method)) {
                boolean activeOnly = !"false".equalsIgnoreCase(request.getParameter("activeOnly"));
                return ok(mediaService.listMedia(site, activeOnly));
            }
            if ("POST".equals(method)) {
                Part filePart = request.getPart("file");
                if (filePart == null) {
                    throw new BadRequestException("Multipart field 'file' is required.");
                }
                try (InputStream input = filePart.getInputStream()) {
                    return created(mediaService.uploadMedia(site, filePart.getSubmittedFileName(), input,
                            filePart.getContentType(), request.getParameter("description")));
                }
            }
            throw unsupported();
        }
        Long mediaId = parseId(path.get(0), "Media id");
        if (path.size() == 2 && "content".equals(path.get(1)) && "GET".equals(method)) {
            return ok(mediaService.getMediaContent(site, mediaId));
        }
        if (path.size() != 1) {
            throw new BadRequestException("Invalid media path.");
        }
        return switch (method) {
            case "GET" -> ok(mediaService.getMedia(site, mediaId));
            case "DELETE" -> ok(mediaService.deleteMedia(site, mediaId));
            default -> throw unsupported();
        };
    }

    private <T> T parseJson(HttpServletRequest request, Class<T> type) {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, type);
        } catch (IOException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid JSON request body.", e);
        }
    }

    private List<String> pathSegments(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return List.of();
        }
        if (!pathInfo.startsWith("/") || pathInfo.endsWith("/") || pathInfo.contains("//")) {
            throw new BadRequestException("Invalid Site administration path.");
        }
        List<String> segments = Arrays.asList(pathInfo.substring(1).split("/", -1));
        if (segments.stream().anyMatch(String::isBlank)) {
            throw new BadRequestException("Invalid Site administration path.");
        }
        return segments;
    }

    private Long parseId(String value, String label) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException(label + " must be a number.", e);
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

    private void writeMediaContent(HttpServletResponse response, MediaContent mediaContent) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(mediaContent.getMimeType());
        response.setContentLengthLong(mediaContent.getFileSize());
        response.setHeader("Content-Disposition", "inline; filename=\"" + sanitizeHeaderValue(
                mediaContent.getOriginalFileName()) + "\"");
        response.getOutputStream().write(mediaContent.getContent());
    }

    private String sanitizeHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return "media";
        }
        return value.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    private void writeSiteError(HttpServletResponse response, SiteServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case SiteService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case SiteService.SITE_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case SiteService.DUPLICATE_SITE_CODE -> HttpServletResponse.SC_CONFLICT;
            case SiteUsageService.SITE_IN_USE -> HttpServletResponse.SC_CONFLICT;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        if (e.getDetails() == null) {
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
            return;
        }
        writeErrorResponse(response, status, e.getCode(), e.getMessage(), e.getDetails());
    }

    private void writePageError(HttpServletResponse response, PageServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case PageService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case PageService.PAGE_NOT_FOUND, PageService.TEMPLATE_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case PageService.DUPLICATE_SLUG -> HttpServletResponse.SC_CONFLICT;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        writeErrorResponse(response, status, e.getCode(), e.getMessage());
    }

    private void writeBlockError(HttpServletResponse response, PageBlockServiceException e) throws IOException {
        int status = PageBlockService.VALIDATION_ERROR.equals(e.getCode())
                ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_NOT_FOUND;
        writeErrorResponse(response, status, e.getCode(), e.getMessage());
    }

    private void writeMenuError(HttpServletResponse response, MenuServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case MenuService.VALIDATION_ERROR, MenuItemService.INVALID_PARENT,
                    MenuItemService.INVALID_PAGE_TARGET, MenuItemService.INVALID_APPLICATION_TARGET -> HttpServletResponse.SC_BAD_REQUEST;
            case MenuService.MENU_NOT_FOUND, MenuItemService.MENU_ITEM_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case MenuService.DUPLICATE_CODE -> HttpServletResponse.SC_CONFLICT;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        writeErrorResponse(response, status, e.getCode(), e.getMessage());
    }

    private void writeApplicationError(HttpServletResponse response, ApplicationServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case ApplicationService.VALIDATION_ERROR, ApplicationService.UNSUPPORTED_APPLICATION -> HttpServletResponse.SC_BAD_REQUEST;
            case ApplicationService.APPLICATION_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case ApplicationService.DUPLICATE_APPLICATION_CODE -> HttpServletResponse.SC_CONFLICT;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        writeErrorResponse(response, status, e.getCode(), e.getMessage());
    }

    private void writeSettingsError(HttpServletResponse response, SiteSettingsServiceException e)
            throws IOException {
        writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getCode(), e.getMessage());
    }

    private void writeMediaError(HttpServletResponse response, MediaServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case MediaService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case MediaService.MEDIA_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        writeErrorResponse(response, status, e.getCode(), e.getMessage());
    }

    private static ResponsePayload ok(Object data) {
        return new ResponsePayload(HttpServletResponse.SC_OK, data);
    }

    private static ResponsePayload created(Object data) {
        return new ResponsePayload(HttpServletResponse.SC_CREATED, data);
    }

    private static BadRequestException unsupported() {
        return new BadRequestException("HTTP method is not supported for this path.");
    }

    private record ResponsePayload(int status, Object data) {
    }

    private static final class BadRequestException extends RuntimeException {
        private BadRequestException(String message) { super(message); }
        private BadRequestException(String message, Throwable cause) { super(message, cause); }
    }
}
