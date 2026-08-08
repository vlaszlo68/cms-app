package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageType;

/**
 * Limited response contract for rendering a published public content page.
 *
 * <p>This DTO deliberately excludes publication state, SEO metadata, administration flags,
 * blocks, media, and other internal page details.</p>
 */
public class PublicPageResponse {

    private final Long id;
    private final String title;
    private final String slug;
    private final PageType pageType;
    private final String templateCode;
    private final String content;

    public PublicPageResponse(Long id, String title, String slug, PageType pageType, String templateCode,
                              String content) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.pageType = pageType;
        this.templateCode = templateCode;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public PageType getPageType() {
        return pageType;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getContent() {
        return content;
    }
}
