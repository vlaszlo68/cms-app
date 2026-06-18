package hu.laci.cms.backend.dao.settings;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.settings.SiteSettingsProperty;

import java.util.Optional;

/**
 * Persistence contract for the single global site settings record.
 */
public interface SiteSettingsDao extends CrudDao<SiteSettings, SiteSettingsProperty> {

    Optional<SiteSettings> findSettings();
}
