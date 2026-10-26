package hu.laci.cms.backend.dao.settings;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.settings.SiteSettingsProperty;
import hu.laci.cms.backend.model.site.Site;

import java.util.Optional;

/**
 * JDBC DAO for per-Site settings entities.
 */
public class SiteSettingsDaoImpl extends BaseDao<SiteSettings, SiteSettingsProperty> implements SiteSettingsDao {

    public SiteSettingsDaoImpl() {
        super(SiteSettings.class);
    }

    @Override
    public Optional<SiteSettings> findBySite(Site site) {
        return findAll(QuerySpec.<SiteSettingsProperty>create()
                .where(SiteSettingsProperty.SITE).equalsTo(site)
                .orderBy(SiteSettingsProperty.ID.asc()))
                .stream()
                .findFirst();
    }
}
