package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageStatus;

/**
 * Lightweight API response DTO for CMS page list endpoints.
 */
public class PageListResponse {

    private final Long id;
    private final String title;
    private final String slug;
    private final PageStatus status;
    private final String metaTitle;
    private final String metaDescription;
    private final Boolean homepage;
    private final Boolean menuVisible;
    private final String createdAt;
    private final String updatedAt;

    public PageListResponse(Long id, String title, String slug, PageStatus status, String metaTitle,
                            String metaDescription, Boolean homepage, Boolean menuVisible, String createdAt,
                            String updatedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.status = status;
        this.metaTitle = metaTitle;
        this.metaDescription = metaDescription;
        this.homepage = homepage;
        this.menuVisible = menuVisible;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
