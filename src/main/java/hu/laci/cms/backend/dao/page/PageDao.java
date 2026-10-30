package hu.laci.cms.backend.dao.page;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageProperty;
import hu.laci.cms.backend.model.site.Site;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * DAO contract for {@link Page} persistence and page-specific lookups.
 */
public interface PageDao extends CrudDao<Page, PageProperty> {

    /**
     * Counts pages directly owned by a Site.
     *
     * @param site owning persisted Site
     * @return number of pages owned by the Site
     */
    long countBySite(Site site);

    /**
     * Lists pages owned by one Site.
     *
     * @param site owning Site with a persisted id
     * @return Site-owned pages
     */
    List<Page> findAllBySite(Site site);

    /**
     * Finds a page id only within one Site.
     *
     * @param site owning Site with a persisted id
     * @param id page id
     * @return matching Site-owned page, if present
     */
    Optional<Page> findBySiteAndId(Site site, Long id);

    /**
     * Finds a slug only within one Site.
     *
     * @param site owning Site with a persisted id
     * @param slug Site-scoped slug
     * @return matching Site-owned page, if present
     */
    Optional<Page> findBySiteAndSlug(Site site, String slug);

    /**
     * Finds the homepage configured within one Site.
     *
     * @param site owning Site with a persisted id
     * @return Site homepage, if present
     */
    Optional<Page> findHomepageBySite(Site site);

    /**
     * Finds pages that can be rendered by the anonymous public site from a bounded id set.
     *
     * @param site owning Site with a persisted id
     * @param pageIds page ids referenced by public menu items
     * @return published content pages from the supplied ids and Site
     */
    List<Page> findPublishedContentBySiteAndIds(Site site, Collection<Long> pageIds);
}
