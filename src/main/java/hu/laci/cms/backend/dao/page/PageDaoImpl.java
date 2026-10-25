package hu.laci.cms.backend.dao.page;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageProperty;
import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;
import hu.laci.cms.backend.model.site.Site;

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
    public List<Page> findAllBySite(Site site) {
        return findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.SITE).equalsTo(site)
                .orderBy(PageProperty.TITLE.asc()));
    }

    @Override
    public Optional<Page> findBySiteAndId(Site site, Long id) {
        return findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.SITE).equalsTo(site)
                .where(PageProperty.ID).equalsTo(id))
                .stream().findFirst();
    }

    @Override
    public Optional<Page> findBySiteAndSlug(Site site, String slug) {
        return findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.SITE).equalsTo(site)
                .where(PageProperty.SLUG).equalsTo(slug))
                .stream().findFirst();
    }

    @Override
    public Optional<Page> findHomepageBySite(Site site) {
        return findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.SITE).equalsTo(site)
                .where(PageProperty.HOMEPAGE).equalsTo(Boolean.TRUE)
                .orderBy(PageProperty.ID.asc()))
                .stream()
                .findFirst();
    }

    @Override
    public List<Page> findPublishedContentBySiteAndIds(Site site, Collection<Long> pageIds) {
        if (pageIds == null || pageIds.isEmpty()) {
            return List.of();
        }
        return findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.SITE).equalsTo(site)
                .where(PageProperty.ID).in(pageIds)
                .where(PageProperty.STATUS).equalsTo(PageStatus.PUBLISHED)
                .where(PageProperty.PAGE_TYPE).equalsTo(PageType.CONTENT)
                .orderBy(PageProperty.ID.asc()));
    }
}
