package hu.laci.cms.backend.dao.application;
import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.application.SiteApplicationProperty;
import hu.laci.cms.backend.model.site.Site;
import java.util.List;
import java.util.Optional;
/** Persistence boundary for Site-owned applications. */
public interface SiteApplicationDao extends CrudDao<SiteApplication, SiteApplicationProperty> {
    /**
     * Deletes all lifecycle-dependent application configurations for one Site.
     *
     * <p>Callers must first establish that no Menu content remains, because MenuItem may refer
     * to a SiteApplication and Menu content is intentionally not cascade-deleted.</p>
     *
     * @param site owning persisted Site
     */
    void deleteBySite(Site site);
    /**
     * Lists applications configured for exactly one Site.
     *
     * @param site owning Site
     * @return Site-owned applications ordered by code
     */
    List<SiteApplication> findAllBySite(Site site);
    /**
     * Resolves an application only when its identifier belongs to the supplied Site.
     *
     * @param site owning Site
     * @param id application identifier
     * @return matching application when Site ownership matches
     */
    Optional<SiteApplication> findBySiteAndId(Site site, Long id);
    /**
     * Resolves an application code only within the supplied Site namespace.
     *
     * @param site owning Site
     * @param code configured application code
     * @return matching Site-owned application when present
     */
    Optional<SiteApplication> findBySiteAndApplicationCode(Site site, String applicationCode);
}
