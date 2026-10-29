package hu.laci.cms.backend.servlet.site;

import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.dto.site.ThemeDefinitionResponse;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.site.ThemeDefinition;
import hu.laci.cms.backend.service.site.ThemeDefinitionRegistry;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Exposes the trusted Site theme definitions to authenticated administrators.
 * Theme definitions are logical identifiers only; frontend applications own stylesheet implementation.
 */
@WebServlet("/api/admin/theme-definitions")
public class ThemeDefinitionServlet extends JsonServletSupport {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        writeJsonResponse(response, HttpServletResponse.SC_OK, ThemeDefinitionRegistry.definitions().stream()
                .map(this::toResponse).toList());
    }

    private ThemeDefinitionResponse toResponse(ThemeDefinition definition) {
        return new ThemeDefinitionResponse(definition.code(), definition.name());
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
}
