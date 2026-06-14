package hu.laci.cms.backend.model.page;

import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.AuditableEntity;

@DbTable("pages")
public class Page extends AuditableEntity {

    @DbColumn("title")
    private String title;

    @DbColumn("slug")
    private String slug;

    @DbColumn("content")
    private String content;

    @DbColumn("status")
    private PageStatus status = PageStatus.DRAFT;

    @DbColumn("meta_title")
    private String metaTitle;

    @DbColumn("meta_description")
    private String metaDescription;

    @DbColumn("homepage")
    private Boolean homepage = Boolean.FALSE;

    @DbColumn("menu_visible")
    private Boolean menuVisible = Boolean.TRUE;

    public Page() {
    }

    public Page(Long id, String title, String slug, String content, PageStatus status, String metaTitle,
                String metaDescription, Boolean homepage, Boolean menuVisible) {
        setId(id);
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PageStatus getStatus() {
        return status;
    }

    public void setStatus(PageStatus status) {
        this.status = status;
    }

    public String getMetaTitle() {
        return metaTitle;
    }

    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public Boolean getHomepage() {
        return homepage;
    }

    public void setHomepage(Boolean homepage) {
        this.homepage = homepage;
    }

    public Boolean getMenuVisible() {
        return menuVisible;
    }

    public void setMenuVisible(Boolean menuVisible) {
        this.menuVisible = menuVisible;
    }
}
