package hu.laci.cms.backend.config.database;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet context listener that owns datasource lifecycle.
 * <p>
 * It initializes {@link DatabaseConfig} when the web application starts and
 * shuts it down when the application is undeployed or the container stops.
 */
public class DatabaseConfigListener implements ServletContextListener {

    /**
     * Initializes database configuration at web application startup.
     *
     * @param sce servlet context event
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DatabaseConfig.initialize(sce.getServletContext());
    }

    /**
     * Shuts down database configuration at web application stop.
     *
     * @param sce servlet context event
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DatabaseConfig.shutdown();
    }
}
