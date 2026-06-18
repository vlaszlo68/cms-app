package hu.laci.cms.backend.dao.page;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.page.PageBlockProperty;

import java.util.List;

/**
 * JDBC page block DAO using generated CRUD and ordered page-specific queries.
 */
public class PageBlockDaoImpl extends BaseDao<PageBlock, PageBlockProperty> implements PageBlockDao {

    public PageBlockDaoImpl() {
        super(PageBlock.class);
    }

    @Override
    public List<PageBlock> findByPageId(Long pageId) {
        return findAll(QuerySpec.<PageBlockProperty>create()
                .where(PageBlockProperty.PAGE_ID).equalsTo(pageId)
                .orderBy(PageBlockProperty.SORT_ORDER.asc())
                .orderBy(PageBlockProperty.ID.asc()));
    }

    @Override
    public List<PageBlock> findVisibleByPageId(Long pageId) {
        return findAll(QuerySpec.<PageBlockProperty>create()
                .where(PageBlockProperty.PAGE_ID).equalsTo(pageId)
                .where(PageBlockProperty.VISIBLE).equalsTo(Boolean.TRUE)
                .orderBy(PageBlockProperty.SORT_ORDER.asc())
                .orderBy(PageBlockProperty.ID.asc()));
    }
}
