package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;

/**
 * Request DTO for creating a CMS page.
 */
public class CreatePageRequest extends PageRequestBase {

    public CreatePageRequest() {
    }

    public CreatePageRequest(String title, String slug, String content, PageStatus status, String metaTitle,
                             String metaDescription, Boolean homepage, Boolean menuVisible) {
        super(title, slug, content, status, metaTitle, metaDescription, homepage, menuVisible);
    }

    public CreatePageRequest(String title, String slug, String content, PageStatus status, String metaTitle,
                             String metaDescription, Boolean homepage, Boolean menuVisible, Long templateId) {
        super(title, slug, content, status, metaTitle, metaDescription, homepage, menuVisible, templateId);
    }

    public CreatePageRequest(String title, String slug, String content, PageType pageType, PageStatus status,
                             String metaTitle, String metaDescription, Boolean homepage, Boolean menuVisible,
                             Long templateId) {
        super(title, slug, content, pageType, status, metaTitle, metaDescription, homepage, menuVisible, templateId);
    }
}
