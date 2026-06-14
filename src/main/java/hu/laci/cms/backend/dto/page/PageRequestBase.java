package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageStatus;

/**
 * Shared fields for CMS page create and update request DTOs.
 */
public abstract class PageRequestBase {

    private String title;
    private String slug;
    private String content;
    private PageStatus status;
    private String metaTitle;
    private String metaDescription;
    private Boolean homepage;
    private Boolean menuVisible;

    protected PageRequestBase() {
    }

    protected PageRequestBase(String title, String slug, String content, PageStatus status, String metaTitle,
                              String metaDescription, Boolean homepage, Boolean menuVisible) {
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.status = status;
        this.metaTitle = metaTitle;
        this.metaDescription = metaDescription;
        this.homepage = homepage;
        this.menuVisible = menuVisible;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getContent() {
        return content;
    }

    public PageStatus getStatus() {
        return status;
    }

    public String getMetaTitle() {
        return metaTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public Boolean getHomepage() {
        return homepage;
    }

    public Boolean getMenuVisible() {
        return menuVisible;
    }
}
