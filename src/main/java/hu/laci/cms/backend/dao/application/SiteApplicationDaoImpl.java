package hu.laci.cms.backend.dao.application;
import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.application.SiteApplicationProperty;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.site.Site;
import java.util.List;
import java.util.Optional;
/** JDBC Application DAO using the shared QuerySpec infrastructure. */
public class SiteApplicationDaoImpl extends BaseDao<SiteApplication, SiteApplicationProperty> implements SiteApplicationDao {
    /** Creates the DAO using Application mapping metadata. */
    public SiteApplicationDaoImpl() { super(SiteApplication.class); }
    /** {@inheritDoc} */
    public void deleteBySite(Site site) { executeCustomUpdate("deleteSiteApplicationsBySite", "DELETE FROM site_applications WHERE site_id = ?", List.of(site), "Failed to delete Site applications."); }
    /** {@inheritDoc} */
    public List<SiteApplication> findAllBySite(Site site) { return findAll(QuerySpec.<SiteApplicationProperty>create().where(SiteApplicationProperty.SITE).equalsTo(site).orderBy(SiteApplicationProperty.APPLICATION_CODE.asc())); }
    /** {@inheritDoc} */
    public Optional<SiteApplication> findBySiteAndId(Site site, Long id) { return findAll(QuerySpec.<SiteApplicationProperty>create().where(SiteApplicationProperty.SITE).equalsTo(site).where(SiteApplicationProperty.ID).equalsTo(id)).stream().findFirst(); }
    /** {@inheritDoc} */
    public Optional<SiteApplication> findBySiteAndApplicationCode(Site site, String applicationCode) { return findAll(QuerySpec.<SiteApplicationProperty>create().where(SiteApplicationProperty.SITE).equalsTo(site).where(SiteApplicationProperty.APPLICATION_CODE).equalsTo(applicationCode)).stream().findFirst(); }
}
