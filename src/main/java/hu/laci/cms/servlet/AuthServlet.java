package hu.laci.cms.servlet;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.dao.UserDaoImpl;
import hu.laci.cms.model.User;
import hu.laci.cms.service.AuthService;
import hu.laci.cms.service.AuthServiceException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@WebServlet("/api/auth/login")
public class AuthServlet extends JsonServletSupport {

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService(new UserDaoImpl());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LoginRequest loginRequest = parseLoginRequest(request);
            validateLoginRequest(loginRequest);

            String loginName = loginRequest.getLoginName().trim();
            String password = loginRequest.getPassword();

            Optional<User> userOptional = authService.login(loginName, password);
            if (userOptional.isEmpty()) {
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return;
            }

            User user = userOptional.get();
            createSession(request, user);
            writeJsonResponse(response, HttpServletResponse.SC_OK, new LoginResponse(
                    user.getId(),
                    user.getLoginName(),
                    user.getEmailAddress()
            ));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (AuthServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        } catch (RuntimeException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
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

    private void createSession(HttpServletRequest request, User user) {
        request.getSession(true).setAttribute("user", user);
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        writeJsonResponse(response, status, Map.of("error", message));
    }

    private static final class LoginResponse {

        private final Long id;
        private final String loginName;
        private final String email;

        private LoginResponse(Long id, String loginName, String email) {
            this.id = id;
            this.loginName = loginName;
            this.email = email;
        }
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
