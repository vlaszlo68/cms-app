package hu.laci.cms.backend.dto.page;

/**
 * Limited public representation of one visible block on a published BLOCK page.
 *
 * <p>This contract deliberately excludes the internal page id. The visibility flag confirms that
 * the block passed the public visibility filter, and the opaque configuration text is preserved
 * for frontend-specific rendering.</p>
 */
public class PublicPageBlockResponse {

    private final Long id;
    private final String blockType;
    private final String title;
    private final Integer sortOrder;
    private final boolean visible;
    private final String configJson;

    /**
     * Creates one public block response.
     *
     * @param id persistent block identifier
     * @param blockType frontend-recognized block type
     * @param title optional human-readable block title
     * @param sortOrder stable public rendering order
     * @param visible whether the block is public; public block lists contain only {@code true}
     * @param configJson opaque frontend block configuration
     */
    public PublicPageBlockResponse(Long id, String blockType, String title, Integer sortOrder, boolean visible,
                                   String configJson) {
        this.id = id;
        this.blockType = blockType;
        this.title = title;
        this.sortOrder = sortOrder;
        this.visible = visible;
        this.configJson = configJson;
    }

    public Long getId() {
        return id;
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

    /**
     * Returns whether this public block is visible.
     *
     * @return {@code true}; hidden blocks are not included in public page responses
     */
    public boolean isVisible() {
        return visible;
    }

    public String getConfigJson() {
        return configJson;
    }
}
