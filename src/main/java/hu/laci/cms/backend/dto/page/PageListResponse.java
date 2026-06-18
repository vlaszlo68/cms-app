package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;

/**
 * Lightweight API response DTO for CMS page list endpoints.
 */
public class PageListResponse {

    private final Long id;
    private final String title;
    private final String slug;
    private final PageStatus status;
    private final PageType pageType;
    private final String metaTitle;
    private final String metaDescription;
    private final Boolean homepage;
    private final Boolean menuVisible;
    private final Long templateId;
    private final String createdAt;
    private final String updatedAt;

    public PageListResponse(Long id, String title, String slug, PageStatus status, String metaTitle,
                            String metaDescription, Boolean homepage, Boolean menuVisible, String createdAt,
                            String updatedAt) {
        this(id, title, slug, status, metaTitle, metaDescription, homepage, menuVisible, null, createdAt,
                updatedAt);
    }

    public PageListResponse(Long id, String title, String slug, PageStatus status, String metaTitle,
                            String metaDescription, Boolean homepage, Boolean menuVisible, Long templateId,
                            String createdAt, String updatedAt) {
        this(id, title, slug, PageType.CONTENT, status, metaTitle, metaDescription, homepage, menuVisible,
                templateId, createdAt, updatedAt);
    }

    public PageListResponse(Long id, String title, String slug, PageType pageType, PageStatus status,
                            String metaTitle, String metaDescription, Boolean homepage, Boolean menuVisible,
                            Long templateId, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.status = status;
        this.pageType = pageType;
        this.metaTitle = metaTitle;
        this.metaDescription = metaDescription;
        this.homepage = homepage;
        this.menuVisible = menuVisible;
        this.templateId = templateId;
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

    public PageType getPageType() {
        return pageType;
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

    public Long getTemplateId() {
        return templateId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
