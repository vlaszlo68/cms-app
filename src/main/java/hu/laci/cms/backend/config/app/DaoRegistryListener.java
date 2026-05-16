package hu.laci.cms.backend.config.app;

import hu.laci.cms.backend.dao.common.DaoRegistry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DaoRegistryListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DaoRegistry.initialize();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DaoRegistry.shutdown();
    }
}
