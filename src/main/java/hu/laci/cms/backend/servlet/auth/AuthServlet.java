package hu.laci.cms.backend.servlet.auth;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.auth.AuthUserResponse;
import hu.laci.cms.backend.dto.auth.LoginRequest;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.service.AuthService;
import hu.laci.cms.backend.service.AuthServiceException;
import hu.laci.cms.backend.servlet.support.CsrfTokenSupport;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

/**
 * Login endpoint for session-based authentication.
 * <p>
 * Successful login rotates the session id, stores an {@link AuthenticatedUser}
 * in the session, creates a CSRF token, and returns an {@link AuthUserResponse}
 * inside the common JSON response envelope.
 */
@WebServlet("/api/auth/login")
public class AuthServlet extends JsonServletSupport {

    private AuthService authService;

    /**
     * Resolves dependencies needed by the login endpoint.
     *
     * @throws ServletException when servlet initialization fails
     */
    @Override
    public void init() throws ServletException {
        UserDao userDao = DaoRegistry.getDao(User.class);
        this.authService = new AuthService(userDao);
    }

    /**
     * Handles {@code POST /api/auth/login}.
     *
     * @param request HTTP request containing a {@link LoginRequest} JSON body
     * @param response HTTP response
     * @throws IOException when reading or writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LoginRequest loginRequest = parseLoginRequest(request);
            validateLoginRequest(loginRequest);

            String loginName = loginRequest.getLoginName().trim();
            String password = loginRequest.getPassword();

            Optional<User> userOptional = authService.login(loginName, password);
            if (userOptional.isEmpty()) {
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "INVALID_CREDENTIALS", "Invalid credentials");
                return;
            }

            User user = userOptional.get();
            AuthenticatedUser authenticatedUser = createSession(request, user);
            String csrfToken = CsrfTokenSupport.ensureToken(request.getSession(false));
            writeJsonResponse(response, HttpServletResponse.SC_OK, new AuthUserResponse(authenticatedUser, csrfToken));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (AuthServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR", "Internal server error.");
        } catch (RuntimeException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR", "Internal server error.");
        }
    }

    private LoginRequest parseLoginRequest(HttpServletRequest request) {
        try (var reader = request.getReader()) {
            return gson.fromJson(reader, LoginRequest.class);
        } catch (IOException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid JSON request body.", e);
        }
    }

    private void validateLoginRequest(LoginRequest loginRequest) {
        if (loginRequest == null
                || isBlank(loginRequest.getLoginName())
                || isBlank(loginRequest.getPassword())) {
            throw new BadRequestException("loginName and password are required.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private AuthenticatedUser createSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getLoginName(),
                user.getEmailAddress()
        );
        session.setAttribute("user", authenticatedUser);
        session.setAttribute(CsrfTokenSupport.SESSION_ATTRIBUTE, CsrfTokenSupport.createToken());
        return authenticatedUser;
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
