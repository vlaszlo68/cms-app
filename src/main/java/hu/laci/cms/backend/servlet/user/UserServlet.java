package hu.laci.cms.backend.servlet.user;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.user.CreateUserRequest;
import hu.laci.cms.backend.dto.user.UpdateUserRequest;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.user.UserService;
import hu.laci.cms.backend.service.user.UserServiceException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * JSON servlet exposing administrator-only user CRUD endpoints.
 */
@WebServlet(urlPatterns = {"/api/users", "/api/users/*"})
public class UserServlet extends JsonServletSupport {

    private UserService userService;

    /**
     * Resolves servlet dependencies.
     *
     * @throws ServletException when initialization fails
     */
    @Override
    public void init() throws ServletException {
        UserDao userDao = DaoRegistry.getDao(User.class);
        this.userService = new UserService(userDao);
    }

    /**
     * Handles {@code GET /api/users} and {@code GET /api/users/{id}}.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException when writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            Long id = parseOptionalId(request);
            if (id == null) {
                writeJsonResponse(response, HttpServletResponse.SC_OK, userService.findAll());
                return;
            }

            writeJsonResponse(response, HttpServletResponse.SC_OK, userService.findById(id));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (UserServiceException e) {
            writeServiceError(response, e);
        }
    }

    /**
     * Handles {@code POST /api/users}.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException when reading or writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            requireCollectionPath(request);
            CreateUserRequest createRequest = parseJson(request, CreateUserRequest.class);
            writeJsonResponse(response, HttpServletResponse.SC_CREATED, userService.create(createRequest));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (UserServiceException e) {
            writeServiceError(response, e);
        }
    }

    /**
     * Handles {@code PUT /api/users/{id}}.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException when reading or writing fails
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            Long id = parseRequiredId(request);
            UpdateUserRequest updateRequest = parseJson(request, UpdateUserRequest.class);
            writeJsonResponse(response, HttpServletResponse.SC_OK, userService.update(id, updateRequest));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (UserServiceException e) {
            writeServiceError(response, e);
        }
    }

    /**
     * Handles {@code DELETE /api/users/{id}} as soft deactivation.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException when writing fails
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            Long id = parseRequiredId(request);
            writeJsonResponse(response, HttpServletResponse.SC_OK, userService.deactivate(id));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (UserServiceException e) {
            writeServiceError(response, e);
        }
    }

    private <T> T parseJson(HttpServletRequest request, Class<T> requestType) {
        try (var reader = request.getReader()) {
            return gson.fromJson(reader, requestType);
        } catch (IOException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid JSON request body.", e);
        }
    }

    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof AuthenticatedUser authenticatedUser)) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_REQUIRED", "Authentication required");
            return false;
        }

        if (authenticatedUser.getRole() != UserRole.ADMIN) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    "FORBIDDEN", "Administrator role is required.");
            return false;
        }

        return true;
    }

    private void requireCollectionPath(HttpServletRequest request) {
        if (parsePathInfo(request) != null) {
            throw new BadRequestException("Endpoint does not accept a user id.");
        }
    }

    private Long parseOptionalId(HttpServletRequest request) {
        String pathInfo = parsePathInfo(request);
        if (pathInfo == null) {
            return null;
        }

        return parseId(pathInfo);
    }

    private Long parseRequiredId(HttpServletRequest request) {
        String pathInfo = parsePathInfo(request);
        if (pathInfo == null) {
            throw new BadRequestException("User id is required.");
        }

        return parseId(pathInfo);
    }

    private String parsePathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return null;
        }
        if (!pathInfo.startsWith("/") || pathInfo.indexOf('/', 1) >= 0) {
            throw new BadRequestException("Invalid user path.");
        }

        return pathInfo.substring(1);
    }

    private Long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException("User id must be a number.", e);
        }
    }

    private void writeServiceError(HttpServletResponse response, UserServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case UserService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case UserService.USER_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case UserService.DUPLICATE_LOGIN_NAME, UserService.DUPLICATE_EMAIL_ADDRESS -> HttpServletResponse.SC_CONFLICT;
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
