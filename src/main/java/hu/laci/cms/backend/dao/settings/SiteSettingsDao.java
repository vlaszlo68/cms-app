package hu.laci.cms.backend.dao.settings;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.settings.SiteSettingsProperty;
import hu.laci.cms.backend.model.site.Site;

import java.util.Optional;

/**
 * Persistence contract for the one settings record owned by each Site.
 */
public interface SiteSettingsDao extends CrudDao<SiteSettings, SiteSettingsProperty> {

    /**
     * Finds the single settings record owned by a Site.
     *
     * @param site owning Site with a persisted id
     * @return Site settings, if configured
     */
    Optional<SiteSettings> findBySite(Site site);
}
