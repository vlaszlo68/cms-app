package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageStatus;

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
}
