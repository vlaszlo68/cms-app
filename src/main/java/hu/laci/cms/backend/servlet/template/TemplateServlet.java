package hu.laci.cms.backend.servlet.template;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.template.CreateTemplateRequest;
import hu.laci.cms.backend.dto.template.UpdateTemplateRequest;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.template.TemplateService;
import hu.laci.cms.backend.service.template.TemplateServiceException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

/**
 * Exposes administrator-only CRUD endpoints for template configurations.
 */
@WebServlet(urlPatterns = {"/api/templates", "/api/templates/*"})
public class TemplateServlet extends JsonServletSupport {

    private TemplateService templateService;

    @Override
    public void init() throws ServletException {
        TemplateDao templateDao = DaoRegistry.getDao(Template.class);
        templateService = new TemplateService(templateDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            Long id = parseOptionalId(request);
            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    id == null ? templateService.listTemplates() : templateService.getTemplate(id));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (TemplateServiceException e) {
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
                    templateService.createTemplate(parseJson(request, CreateTemplateRequest.class)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (TemplateServiceException e) {
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
                    templateService.updateTemplate(parseRequiredId(request),
                            parseJson(request, UpdateTemplateRequest.class)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (TemplateServiceException e) {
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
                    templateService.deactivateTemplate(parseRequiredId(request)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (TemplateServiceException e) {
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
            throw new BadRequestException("Endpoint does not accept a template id.");
        }
    }

    private Long parseOptionalId(HttpServletRequest request) {
        String path = parsePath(request);
        return path == null ? null : parseId(path);
    }

    private Long parseRequiredId(HttpServletRequest request) {
        Long id = parseOptionalId(request);
        if (id == null) {
            throw new BadRequestException("Template id is required.");
        }
        return id;
    }

    private String parsePath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return null;
        }
        if (!pathInfo.startsWith("/")) {
            throw new BadRequestException("Invalid template path.");
        }
        return pathInfo.substring(1);
    }

    private Long parseId(String value) {
        if (value.isBlank() || value.contains("/")) {
            throw new BadRequestException("Invalid template path.");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Template id must be a number.", e);
        }
    }

    private void writeServiceError(HttpServletResponse response, TemplateServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case TemplateService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case TemplateService.TEMPLATE_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case TemplateService.DUPLICATE_CODE -> HttpServletResponse.SC_CONFLICT;
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
