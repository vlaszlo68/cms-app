package hu.laci.cms.backend.dao.application;
import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.application.ApplicationProperty;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.site.Site;
import java.util.List;
import java.util.Optional;
/** JDBC Application DAO using the shared QuerySpec infrastructure. */
public class ApplicationDaoImpl extends BaseDao<Application, ApplicationProperty> implements ApplicationDao {
    /** Creates the DAO using Application mapping metadata. */
    public ApplicationDaoImpl() { super(Application.class); }
    /** {@inheritDoc} */
    public List<Application> findAllBySite(Site site) { return findAll(QuerySpec.<ApplicationProperty>create().where(ApplicationProperty.SITE).equalsTo(site).orderBy(ApplicationProperty.CODE.asc())); }
    /** {@inheritDoc} */
    public Optional<Application> findBySiteAndId(Site site, Long id) { return findAll(QuerySpec.<ApplicationProperty>create().where(ApplicationProperty.SITE).equalsTo(site).where(ApplicationProperty.ID).equalsTo(id)).stream().findFirst(); }
    /** {@inheritDoc} */
    public Optional<Application> findBySiteAndCode(Site site, String code) { return findAll(QuerySpec.<ApplicationProperty>create().where(ApplicationProperty.SITE).equalsTo(site).where(ApplicationProperty.CODE).equalsTo(code)).stream().findFirst(); }
}
