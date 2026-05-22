package hu.laci.cms.backend.servlet.auth;

import hu.laci.cms.backend.config.security.SecurityConfig;
import hu.laci.cms.backend.dto.auth.AuthConfigResponse;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Public endpoint exposing authentication UI feature flags.
 */
@WebServlet("/api/auth/config")
public class AuthConfigServlet extends JsonServletSupport {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityConfig securityConfig = SecurityConfig.getCurrent();
        writeJsonResponse(response, HttpServletResponse.SC_OK, new AuthConfigResponse(
                securityConfig.isLoginCaptchaEnabled(),
                securityConfig.isRegistrationCaptchaEnabled()
        ));
    }
}
