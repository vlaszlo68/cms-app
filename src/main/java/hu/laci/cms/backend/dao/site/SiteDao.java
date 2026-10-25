package hu.laci.cms.backend.dao.site;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.site.SiteProperty;

import java.util.Optional;

/**
 * Persistence contract for global Site records.
 */
public interface SiteDao extends CrudDao<Site, SiteProperty> {

    /**
     * Finds a Site by its stable machine-readable code.
     *
     * @param code exact Site code
     * @return matching Site, if present
     */
    Optional<Site> findByCode(String code);
}
