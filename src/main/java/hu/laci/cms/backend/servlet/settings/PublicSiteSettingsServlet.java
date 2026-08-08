package hu.laci.cms.backend.servlet.settings;

import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Exposes read-only global layout settings for anonymous public pages.
 */
@WebServlet(urlPatterns = "/api/public/site-settings")
public class PublicSiteSettingsServlet extends JsonServletSupport {

    private SiteSettingsService siteSettingsService;

    @Override
    public void init() throws ServletException {
        SiteSettingsDao siteSettingsDao = DaoRegistry.getDao(SiteSettings.class);
        siteSettingsService = new SiteSettingsService(siteSettingsDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeJsonResponseIncludingNulls(response, HttpServletResponse.SC_OK, siteSettingsService.getPublicSettings());
    }
}
