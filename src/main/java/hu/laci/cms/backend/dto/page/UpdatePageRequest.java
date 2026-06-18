package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;

/**
 * Request DTO for updating a CMS page.
 */
public class UpdatePageRequest extends PageRequestBase {

    public UpdatePageRequest() {
    }

    public UpdatePageRequest(String title, String slug, String content, PageStatus status, String metaTitle,
                             String metaDescription, Boolean homepage, Boolean menuVisible) {
        super(title, slug, content, status, metaTitle, metaDescription, homepage, menuVisible);
    }

    public UpdatePageRequest(String title, String slug, String content, PageStatus status, String metaTitle,
                             String metaDescription, Boolean homepage, Boolean menuVisible, Long templateId) {
        super(title, slug, content, status, metaTitle, metaDescription, homepage, menuVisible, templateId);
    }

    public UpdatePageRequest(String title, String slug, String content, PageType pageType, PageStatus status,
                             String metaTitle, String metaDescription, Boolean homepage, Boolean menuVisible,
                             Long templateId) {
        super(title, slug, content, pageType, status, metaTitle, metaDescription, homepage, menuVisible, templateId);
    }
}
