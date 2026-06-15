package hu.laci.cms.backend.config.media;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MediaStorageConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        MediaStorageConfig.initialize(sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        MediaStorageConfig.reset();
    }
}
