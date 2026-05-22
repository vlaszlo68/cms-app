package hu.laci.cms.backend.config.security;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet listener that initializes centralized authentication and password policy configuration.
 */
public class SecurityConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SecurityConfig.initialize(sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        SecurityConfig.reset();
    }
}
