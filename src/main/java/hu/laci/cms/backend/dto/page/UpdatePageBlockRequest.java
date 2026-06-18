package hu.laci.cms.backend.dto.page;

/**
 * Request DTO for updating a page block.
 */
public class UpdatePageBlockRequest extends PageBlockRequestBase {

    public UpdatePageBlockRequest() {
    }

    public UpdatePageBlockRequest(Long pageId, String blockType, String title, Integer sortOrder, Boolean visible,
                                  String configJson) {
        super(pageId, blockType, title, sortOrder, visible, configJson);
    }
}
