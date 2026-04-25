package hu.laci.cms.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.laci.cms.dao.UserDaoImpl;
import hu.laci.cms.model.User;
import hu.laci.cms.service.AuthService;
import hu.laci.cms.service.AuthServiceException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@WebServlet("/login")
public class AuthServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                return;
            }

            createSession(request, userOptional.get());
            writeJsonResponse(response, HttpServletResponse.SC_OK, Map.of("status", "ok"));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (AuthServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        } catch (RuntimeException e) {
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }

    private LoginRequest parseLoginRequest(HttpServletRequest request) {
        try {
            return objectMapper.readValue(request.getInputStream(), LoginRequest.class);
        } catch (IOException e) {
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
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        writeJsonResponse(response, status, Map.of("error", message));
    }

    private void writeJsonResponse(HttpServletResponse response, int status, Map<String, String> payload)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        objectMapper.writeValue(response.getWriter(), payload);
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
