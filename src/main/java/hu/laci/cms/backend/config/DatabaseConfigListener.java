package hu.laci.cms.backend.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DatabaseConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DatabaseConfig.initialize(sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DatabaseConfig.shutdown();
    }
}
