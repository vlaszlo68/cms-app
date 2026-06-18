package hu.laci.cms.backend.dto.page;

/**
 * Request DTO for creating a page block.
 */
public class CreatePageBlockRequest extends PageBlockRequestBase {

    public CreatePageBlockRequest() {
    }

    public CreatePageBlockRequest(Long pageId, String blockType, String title, Integer sortOrder, Boolean visible,
                                  String configJson) {
        super(pageId, blockType, title, sortOrder, visible, configJson);
    }
}
