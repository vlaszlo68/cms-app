package hu.laci.cms.backend.config.external;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/** Initializes outbound integration configuration during web application lifecycle. */
public final class ExternalApiConfigListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event) { ExternalApiConfig.initialize(event.getServletContext()); }
    @Override public void contextDestroyed(ServletContextEvent event) { ExternalApiConfig.reset(); }
}
