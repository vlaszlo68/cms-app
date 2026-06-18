package hu.laci.cms.backend.dto.page;

/**
 * API response describing one page block.
 */
public class PageBlockResponse {

    private final Long id;
    private final Long pageId;
    private final String blockType;
    private final String title;
    private final Integer sortOrder;
    private final boolean visible;
    private final String configJson;

    public PageBlockResponse(Long id, Long pageId, String blockType, String title, Integer sortOrder,
                             boolean visible, String configJson) {
        this.id = id;
        this.pageId = pageId;
        this.blockType = blockType;
        this.title = title;
        this.sortOrder = sortOrder;
        this.visible = visible;
        this.configJson = configJson;
    }

    public Long getId() {
        return id;
    }

    public Long getPageId() {
        return pageId;
    }

    public String getBlockType() {
        return blockType;
    }

    public String getTitle() {
        return title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getConfigJson() {
        return configJson;
    }
}
