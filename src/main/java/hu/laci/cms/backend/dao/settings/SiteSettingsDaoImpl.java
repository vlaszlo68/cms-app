package hu.laci.cms.backend.dao.settings;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.settings.SiteSettingsProperty;

import java.util.Optional;

/**
 * JDBC DAO for the singleton site settings entity.
 */
public class SiteSettingsDaoImpl extends BaseDao<SiteSettings, SiteSettingsProperty> implements SiteSettingsDao {

    public SiteSettingsDaoImpl() {
        super(SiteSettings.class);
    }

    @Override
    public Optional<SiteSettings> findSettings() {
        return findAll(QuerySpec.<SiteSettingsProperty>create().orderBy(SiteSettingsProperty.ID.asc()))
                .stream()
                .findFirst();
    }
}
