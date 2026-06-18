package hu.laci.cms.backend.dao.page;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.page.PageBlockProperty;

import java.util.List;

/**
 * Persistence contract for ordered page block configurations.
 */
public interface PageBlockDao extends CrudDao<PageBlock, PageBlockProperty> {

    List<PageBlock> findByPageId(Long pageId);

    List<PageBlock> findVisibleByPageId(Long pageId);
}
