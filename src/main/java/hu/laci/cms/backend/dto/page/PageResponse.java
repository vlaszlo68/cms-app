package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageStatus;
import hu.laci.cms.backend.model.page.PageType;

/**
 * API response DTO for CMS page endpoints.
 */
public class PageResponse {

    private final Long id;
    private final String title;
    private final String slug;
    private final String content;
    private final PageType pageType;
    private final PageStatus status;
    private final String metaTitle;
    private final String metaDescription;
    private final Boolean homepage;
    private final Boolean menuVisible;
    private final Long templateId;
    private final String createdAt;
    private final String updatedAt;

    public PageResponse(Long id, String title, String slug, String content, PageStatus status, String metaTitle,
                        String metaDescription, Boolean homepage, Boolean menuVisible, String createdAt,
                        String updatedAt) {
        this(id, title, slug, content, status, metaTitle, metaDescription, homepage, menuVisible, null,
                createdAt, updatedAt);
    }

    public PageResponse(Long id, String title, String slug, String content, PageStatus status, String metaTitle,
                        String metaDescription, Boolean homepage, Boolean menuVisible, Long templateId,
                        String createdAt, String updatedAt) {
        this(id, title, slug, content, PageType.CONTENT, status, metaTitle, metaDescription, homepage, menuVisible,
                templateId, createdAt, updatedAt);
    }

    public PageResponse(Long id, String title, String slug, String content, PageType pageType, PageStatus status,
                        String metaTitle, String metaDescription, Boolean homepage, Boolean menuVisible,
                        Long templateId, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.pageType = pageType;
        this.status = status;
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

    public String getContent() {
        return content;
    }

    public PageType getPageType() {
        return pageType;
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
