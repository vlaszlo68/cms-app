package hu.laci.cms.backend.servlet.page;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.page.CreatePageBlockRequest;
import hu.laci.cms.backend.dto.page.UpdatePageBlockRequest;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.user.UserRole;
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
 * Exposes administrator-only CRUD endpoints for individual page blocks.
 */
@WebServlet(urlPatterns = {"/api/page-blocks", "/api/page-blocks/*"})
public class PageBlockServlet extends JsonServletSupport {

    private PageBlockService pageBlockService;

    @Override
    public void init() throws ServletException {
        PageDao pageDao = DaoRegistry.getDao(Page.class);
        PageBlockDao pageBlockDao = DaoRegistry.getDao(PageBlock.class);
        pageBlockService = new PageBlockService(pageDao, pageBlockDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    pageBlockService.getBlock(parseRequiredId(request)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageBlockServiceException e) {
            writeServiceError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            requireCollectionPath(request);
            writeJsonResponse(response, HttpServletResponse.SC_CREATED,
                    pageBlockService.createBlock(parseJson(request, CreatePageBlockRequest.class)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageBlockServiceException e) {
            writeServiceError(response, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    pageBlockService.updateBlock(parseRequiredId(request),
                            parseJson(request, UpdatePageBlockRequest.class)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageBlockServiceException e) {
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
                    pageBlockService.deleteBlock(parseRequiredId(request)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageBlockServiceException e) {
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

    private void requireCollectionPath(HttpServletRequest request) {
        if (parsePath(request) != null) {
            throw new BadRequestException("Endpoint does not accept a page block id.");
        }
    }

    private Long parseRequiredId(HttpServletRequest request) {
        String path = parsePath(request);
        if (path == null) {
            throw new BadRequestException("Page block id is required.");
        }
        if (path.contains("/")) {
            throw new BadRequestException("Invalid page block path.");
        }
        try {
            return Long.parseLong(path);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Page block id must be a number.", e);
        }
    }

    private String parsePath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return null;
        }
        if (!pathInfo.startsWith("/")) {
            throw new BadRequestException("Invalid page block path.");
        }
        return pathInfo.substring(1);
    }

    private void writeServiceError(HttpServletResponse response, PageBlockServiceException e) throws IOException {
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
}
