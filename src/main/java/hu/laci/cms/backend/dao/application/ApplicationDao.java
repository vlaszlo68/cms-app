package hu.laci.cms.backend.dao.application;
import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.application.ApplicationProperty;
import hu.laci.cms.backend.model.site.Site;
import java.util.List;
import java.util.Optional;
/** Persistence boundary for Site-owned applications. */
public interface ApplicationDao extends CrudDao<Application, ApplicationProperty> {
    /**
     * Counts applications directly owned by a Site.
     *
     * @param site owning persisted Site
     * @return number of applications owned by the Site
     */
    long countBySite(Site site);
    /**
     * Lists applications configured for exactly one Site.
     *
     * @param site owning Site
     * @return Site-owned applications ordered by code
     */
    List<Application> findAllBySite(Site site);
    /**
     * Resolves an application only when its identifier belongs to the supplied Site.
     *
     * @param site owning Site
     * @param id application identifier
     * @return matching application when Site ownership matches
     */
    Optional<Application> findBySiteAndId(Site site, Long id);
    /**
     * Resolves an application code only within the supplied Site namespace.
     *
     * @param site owning Site
     * @param code configured application code
     * @return matching Site-owned application when present
     */
    Optional<Application> findBySiteAndCode(Site site, String code);
}
