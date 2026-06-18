package hu.laci.cms.backend.dto.page;

import java.util.List;

/**
 * Optional expanded page response containing its ordered block collection.
 */
public class PageWithBlocksResponse {

    private final PageResponse page;
    private final List<PageBlockResponse> blocks;

    public PageWithBlocksResponse(PageResponse page, List<PageBlockResponse> blocks) {
        this.page = page;
        this.blocks = List.copyOf(blocks);
    }

    public PageResponse getPage() {
        return page;
    }

    public List<PageBlockResponse> getBlocks() {
        return blocks;
    }
}
