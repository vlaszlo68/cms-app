package hu.laci.cms.backend.config.app;

import hu.laci.cms.backend.dao.common.DaoRegistry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet context listener that owns DAO registry lifecycle.
 * <p>
 * The registry must be initialized before servlets try to resolve DAO instances
 * through {@link DaoRegistry}.
 */
public class DaoRegistryListener implements ServletContextListener {

    /**
     * Initializes DAO registrations at web application startup.
     *
     * @param sce servlet context event
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DaoRegistry.initialize();
    }

    /**
     * Clears DAO registrations at web application stop.
     *
     * @param sce servlet context event
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DaoRegistry.shutdown();
    }
}
