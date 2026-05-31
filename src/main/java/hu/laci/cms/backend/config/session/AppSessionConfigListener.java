package hu.laci.cms.backend.config.session;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initializes the application session configuration and store.
 */
public class AppSessionConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AppSessionConfig.initialize(sce.getServletContext());
        AppSessionManager.initialize(AppSessionConfig.getCurrent());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        AppSessionManager.reset();
        AppSessionConfig.reset();
    }
}
