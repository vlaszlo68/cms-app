package hu.laci.cms.backend.dto.page;

/**
 * Shared request fields for creating and updating page blocks.
 */
public abstract class PageBlockRequestBase {

    private Long pageId;
    private String blockType;
    private String title;
    private Integer sortOrder;
    private Boolean visible;
    private String configJson;

    protected PageBlockRequestBase() {
    }

    protected PageBlockRequestBase(Long pageId, String blockType, String title, Integer sortOrder, Boolean visible,
                                   String configJson) {
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

    public String getBlockType() {
        return blockType;
    }

    public String getTitle() {
        return title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Boolean getVisible() {
        return visible;
    }

    public String getConfigJson() {
        return configJson;
    }
}
