package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageStatus;

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
}
