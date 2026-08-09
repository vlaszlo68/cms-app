package hu.laci.cms.backend.servlet.page;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.page.CreatePageRequest;
import hu.laci.cms.backend.dto.page.UpdatePageRequest;
import hu.laci.cms.backend.dto.page.PageResponse;
import hu.laci.cms.backend.dto.page.PageWithBlocksResponse;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.page.PageServiceException;
import hu.laci.cms.backend.service.page.PageBlockService;
import hu.laci.cms.backend.service.page.PageBlockServiceException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

/**
 * JSON servlet exposing administrator-only CMS page endpoints.
 */
@WebServlet(urlPatterns = {"/api/pages", "/api/pages/*"})
public class PageServlet extends JsonServletSupport {

    private PageService pageService;
    private PageBlockService pageBlockService;

    @Override
    public void init() throws ServletException {
        PageDao pageDao = DaoRegistry.getDao(Page.class);
        TemplateDao templateDao = DaoRegistry.getDao(Template.class);
        PageBlockDao pageBlockDao = DaoRegistry.getDao(PageBlock.class);
        this.pageService = new PageService(pageDao, templateDao, pageBlockDao);
        this.pageBlockService = new PageBlockService(pageDao, pageBlockDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            PagePath pagePath = parsePagePath(request);
            if (pagePath.slug() != null) {
                writeJsonResponse(response, HttpServletResponse.SC_OK, pageService.getBySlug(pagePath.slug()));
                return;
            }
            if (pagePath.blocks()) {
                writeJsonResponse(response, HttpServletResponse.SC_OK,
                        pageBlockService.listBlocks(pagePath.id()));
                return;
            }
            if (pagePath.id() != null) {
                PageResponse page = pageService.getPage(pagePath.id());
                if ("true".equalsIgnoreCase(request.getParameter("includeBlocks"))) {
                    writeJsonResponse(response, HttpServletResponse.SC_OK,
                            new PageWithBlocksResponse(page, pageBlockService.listBlocks(pagePath.id())));
                    return;
                }
                writeJsonResponse(response, HttpServletResponse.SC_OK, page);
                return;
            }

            writeJsonResponse(response, HttpServletResponse.SC_OK, pageService.listPages());
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageServiceException e) {
            writeServiceError(response, e);
        } catch (PageBlockServiceException e) {
            writeBlockServiceError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            requireCollectionPath(request);
            CreatePageRequest createRequest = parseJson(request, CreatePageRequest.class);
            writeJsonResponse(response, HttpServletResponse.SC_CREATED, pageService.createPage(createRequest));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageServiceException e) {
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
            UpdatePageRequest updateRequest = parseJson(request, UpdatePageRequest.class);
            writeJsonResponse(response, HttpServletResponse.SC_OK, pageService.updatePage(id, updateRequest));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageServiceException e) {
            writeServiceError(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            Long id = parseRequiredId(request);
            writeJsonResponse(response, HttpServletResponse.SC_OK, pageService.deletePage(id));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageServiceException e) {
            writeServiceError(response, e);
        }
    }

    private <T> T parseJson(HttpServletRequest request, Class<T> requestType) {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, requestType);
        } catch (IOException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid JSON request body.", e);
        }
    }

    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<AuthenticatedUser> authenticatedUser = AppSessionManager.getAuthenticatedUser(request, response);
        if (authenticatedUser.isEmpty()) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_REQUIRED", "Authentication required");
            return false;
        }

        if (authenticatedUser.get().getRole() != UserRole.ADMIN) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    "FORBIDDEN", "Administrator role is required.");
            return false;
        }

        return true;
    }

    private void requireCollectionPath(HttpServletRequest request) {
        if (parsePathInfo(request) != null) {
            throw new BadRequestException("Endpoint does not accept a page id.");
        }
    }

    private Long parseRequiredId(HttpServletRequest request) {
        String pathInfo = parsePathInfo(request);
        if (pathInfo == null) {
            throw new BadRequestException("Page id is required.");
        }

        return parseId(pathInfo);
    }

    private PagePath parsePagePath(HttpServletRequest request) {
        String pathInfo = parsePathInfo(request);
        if (pathInfo == null) {
            return new PagePath(null, null, false);
        }

        if (pathInfo.startsWith("slug/")) {
            String slug = pathInfo.substring("slug/".length());
            if (slug.isBlank() || slug.contains("/")) {
                throw new BadRequestException("Invalid page slug path.");
            }
            return new PagePath(null, slug, false);
        }

        if (pathInfo.endsWith("/blocks")) {
            String idPart = pathInfo.substring(0, pathInfo.length() - "/blocks".length());
            return new PagePath(parseId(idPart), null, true);
        }

        return new PagePath(parseId(pathInfo), null, false);
    }

    private String parsePathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return null;
        }
        if (!pathInfo.startsWith("/")) {
            throw new BadRequestException("Invalid page path.");
        }

        return pathInfo.substring(1);
    }

    private Long parseId(String value) {
        if (value.contains("/")) {
            throw new BadRequestException("Invalid page path.");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Page id must be a number.", e);
        }
    }

    private void writeServiceError(HttpServletResponse response, PageServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case PageService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case PageService.PAGE_NOT_FOUND, PageService.TEMPLATE_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case PageService.DUPLICATE_SLUG -> HttpServletResponse.SC_CONFLICT;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        if (e.getValidationErrors().isEmpty()) {
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
            return;
        }
        writeErrorResponse(response, status, e.getCode(), e.getMessage(), e.getValidationErrors());
    }

    private void writeBlockServiceError(HttpServletResponse response, PageBlockServiceException e)
            throws IOException {
        int status = switch (e.getCode()) {
            case PageBlockService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case PageBlockService.PAGE_NOT_FOUND, PageBlockService.PAGE_BLOCK_NOT_FOUND ->
                    HttpServletResponse.SC_NOT_FOUND;
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

    private record PagePath(Long id, String slug, boolean blocks) {
    }
}
