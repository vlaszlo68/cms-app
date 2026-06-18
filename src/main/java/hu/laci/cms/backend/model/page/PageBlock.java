package hu.laci.cms.backend.model.page;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

/**
 * Represents one ordered, frontend-rendered configuration block of a CMS page.
 */
@DbTable("page_blocks")
public class PageBlock extends AuditableEntity {

    @DbColumn("page_id")
    private Long pageId;

    @DbColumn("block_type")
    private String blockType;

    @DbColumn("title")
    private String title;

    @DbColumn("sort_order")
    private Integer sortOrder = 0;

    @DbColumn("visible")
    private boolean visible = true;

    @DbColumn("config_json")
    private String configJson;

    public PageBlock() {
    }

    public PageBlock(Long id, Long pageId, String blockType, String title, Integer sortOrder, boolean visible,
                     String configJson) {
        setId(id);
        this.pageId = pageId;
        this.blockType = blockType;
        this.title = title;
        this.sortOrder = sortOrder;
        this.visible = visible;
        this.configJson = configJson;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
}
