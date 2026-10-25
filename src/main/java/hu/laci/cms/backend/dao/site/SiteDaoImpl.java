package hu.laci.cms.backend.dao.site;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.site.SiteProperty;

import java.util.Optional;

/**
 * JDBC Site DAO backed by the common annotation-driven CRUD infrastructure.
 */
public class SiteDaoImpl extends BaseDao<Site, SiteProperty> implements SiteDao {

    /** Creates a Site DAO using the shared connection and transaction infrastructure. */
    public SiteDaoImpl() {
        super(Site.class);
    }

    @Override
    public Optional<Site> findByCode(String code) {
        return findOneByProperty("code", code, "Failed to find Site by code: " + code);
    }
}
