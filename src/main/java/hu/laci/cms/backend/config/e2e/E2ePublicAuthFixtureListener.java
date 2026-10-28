package hu.laci.cms.backend.config.e2e;

import hu.laci.cms.backend.dao.application.ApplicationDao;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.service.e2e.E2ePublicAuthFixtureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Provisions local public-auth E2E data after migrations and DAO registration.
 */
public final class E2ePublicAuthFixtureListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(E2ePublicAuthFixtureListener.class);

    /**
     * Provisions fixtures only after explicit environment opt-in.
     *
     * @param sce servlet context startup event
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (!E2ePublicAuthFixtures.isEnabled()) {
            return;
        }
        SiteDao siteDao = DaoRegistry.getDao(Site.class);
        UserDao userDao = DaoRegistry.getDao(User.class);
        ApplicationDao applicationDao = DaoRegistry.getDao(Application.class);
        new E2ePublicAuthFixtureService(siteDao, userDao, applicationDao).ensureFixtures();
        LOGGER.warn("Local public-auth E2E fixtures are enabled; reserved E2E users and applications were provisioned.");
    }
}
