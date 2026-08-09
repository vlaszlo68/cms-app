package hu.laci.cms.backend.dto.page;

import hu.laci.cms.backend.model.page.PageType;

import java.util.List;

/**
 * Limited response contract for rendering a published public CONTENT or BLOCK page.
 *
 * <p>This DTO deliberately excludes publication state, SEO metadata, administration flags,
 * media, and other internal page details. CONTENT responses retain the original {@code content}
 * field and omit {@code blocks}; BLOCK responses omit {@code content} and include visible blocks.</p>
 */
public class PublicPageResponse {

    private final Long id;
    private final String title;
    private final String slug;
    private final PageType pageType;
    private final String templateCode;
    private final String content;
    private final List<PublicPageBlockResponse> blocks;

    public PublicPageResponse(Long id, String title, String slug, PageType pageType, String templateCode,
                              String content) {
        this(id, title, slug, pageType, templateCode, content, null);
    }

    /**
     * Creates a public page response for exactly one public page type.
     *
     * @param id persistent page identifier
     * @param title public page title
     * @param slug public page slug
     * @param pageType CONTENT or BLOCK
     * @param templateCode optional template code
     * @param content rendered source content for CONTENT pages, otherwise {@code null}
     * @param blocks visible ordered blocks for BLOCK pages, otherwise {@code null}
     */
    public PublicPageResponse(Long id, String title, String slug, PageType pageType, String templateCode,
                              String content, List<PublicPageBlockResponse> blocks) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.pageType = pageType;
        this.templateCode = templateCode;
        this.content = content;
        this.blocks = blocks == null ? null : List.copyOf(blocks);
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

    /**
     * Returns visible ordered blocks for a BLOCK response, or {@code null} for a CONTENT response.
     *
     * @return public blocks or {@code null}
     */
    public List<PublicPageBlockResponse> getBlocks() {
        return blocks;
    }
}
