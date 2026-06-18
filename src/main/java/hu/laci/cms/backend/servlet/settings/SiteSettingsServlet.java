package hu.laci.cms.backend.servlet.settings;

import com.google.gson.JsonSyntaxException;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.settings.SaveSiteSettingsRequest;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.service.settings.SiteSettingsServiceException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

/**
 * Exposes administrator-only read and update operations for global site settings.
 */
@WebServlet(urlPatterns = "/api/site-settings")
public class SiteSettingsServlet extends JsonServletSupport {

    private SiteSettingsService siteSettingsService;

    @Override
    public void init() throws ServletException {
        SiteSettingsDao siteSettingsDao = DaoRegistry.getDao(SiteSettings.class);
        siteSettingsService = new SiteSettingsService(siteSettingsDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        writeJsonResponse(response, HttpServletResponse.SC_OK, siteSettingsService.getSettings());
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        try {
            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    siteSettingsService.saveSettings(parseJson(request)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (SiteSettingsServiceException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getCode(), e.getMessage());
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

    private SaveSiteSettingsRequest parseJson(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, SaveSiteSettingsRequest.class);
        } catch (IOException | JsonSyntaxException e) {
            throw new BadRequestException("Invalid JSON request body.", e);
        }
    }

    private static final class BadRequestException extends RuntimeException {
        private BadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
