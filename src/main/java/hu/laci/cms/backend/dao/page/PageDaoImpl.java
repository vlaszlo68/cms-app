package hu.laci.cms.backend.dao.page;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageProperty;
import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link PageDao} based on {@link BaseDao}.
 */
public class PageDaoImpl extends BaseDao<Page, PageProperty> implements PageDao {

    public PageDaoImpl() {
        super(Page.class);
    }

    @Override
    public Optional<Page> findBySlug(String slug) {
        return findOneByProperty("slug", slug, "Failed to find page by slug: " + slug);
    }

    @Override
    public Optional<Page> findHomepage() {
        return findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.HOMEPAGE).equalsTo(Boolean.TRUE)
                .orderBy(PageProperty.ID.asc()))
                .stream()
                .findFirst();
    }

    @Override
    public List<Page> findPublishedContentByIds(Collection<Long> pageIds) {
        if (pageIds == null || pageIds.isEmpty()) {
            return List.of();
        }
        return findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.ID).in(pageIds)
                .where(PageProperty.STATUS).equalsTo(PageStatus.PUBLISHED)
                .where(PageProperty.PAGE_TYPE).equalsTo(PageType.CONTENT)
                .orderBy(PageProperty.ID.asc()));
    }
}
