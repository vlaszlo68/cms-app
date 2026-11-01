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
    public List<PageBlock> findAllBlocks() {
        return findAll(QuerySpec.<PageBlockProperty>create().orderBy(PageBlockProperty.ID.asc()));
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

    @Override
    public List<PageBlock> findBySite(hu.laci.cms.backend.model.site.Site site) {
        String projection = "pb.id AS page_blocks_id, pb.page_id AS page_blocks_page_id, "
                + "pb.block_type AS page_blocks_block_type, pb.title AS page_blocks_title, "
                + "pb.sort_order AS page_blocks_sort_order, pb.visible AS page_blocks_visible, "
                + "pb.config_json AS page_blocks_config_json, pb.created_at AS page_blocks_created_at, "
                + "pb.updated_at AS page_blocks_updated_at, pb.created_by AS page_blocks_created_by, "
                + "pb.updated_by AS page_blocks_updated_by";
        return findCustomList("findBlocksBySite", "SELECT " + projection
                + " FROM page_blocks pb JOIN pages p ON p.id = pb.page_id WHERE p.site_id = ?",
                java.util.List.of(site), this::mapEntity, "Failed to find Site page blocks.");
    }
}
