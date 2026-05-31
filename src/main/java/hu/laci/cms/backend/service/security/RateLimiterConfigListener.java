package hu.laci.cms.backend.service.security;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initializes shared rate limiter configuration.
 */
public class RateLimiterConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        RateLimiterConfig.initialize(sce.getServletContext());
        RateLimiterManager.initialize(RateLimiterConfig.getCurrent());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        RateLimiterManager.reset();
        RateLimiterConfig.reset();
    }
}
