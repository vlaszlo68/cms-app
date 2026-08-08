package hu.laci.cms.backend.dao.page;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageProperty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * DAO contract for {@link Page} persistence and page-specific lookups.
 */
public interface PageDao extends CrudDao<Page, PageProperty> {

    Optional<Page> findBySlug(String slug);

    Optional<Page> findHomepage();

    /**
     * Finds pages that can be rendered by the anonymous public site from a bounded id set.
     *
     * @param pageIds page ids referenced by public menu items
     * @return published content pages from the supplied ids
     */
    List<Page> findPublishedContentByIds(Collection<Long> pageIds);
}
