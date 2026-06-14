package hu.laci.cms.backend.service.page;

import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dto.page.CreatePageRequest;
import hu.laci.cms.backend.dto.page.PageResponse;
import hu.laci.cms.backend.dto.page.UpdatePageRequest;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageProperty;
import hu.laci.cms.backend.model.page.PageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Business service for CMS page management.
 */
public class PageService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String PAGE_NOT_FOUND = "PAGE_NOT_FOUND";
    public static final String DUPLICATE_SLUG = "DUPLICATE_SLUG";

    private static final Logger LOGGER = LoggerFactory.getLogger(PageService.class);

    private final PageDao pageDao;

    public PageService(PageDao pageDao) {
        this.pageDao = Objects.requireNonNull(pageDao, "pageDao must not be null");
    }

    public List<PageResponse> listPages() {
        return pageDao.findAll(QuerySpec.<PageProperty>create().orderBy(PageProperty.TITLE.asc()))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse getPage(Long id) {
        return toResponse(loadPage(id));
    }

    public PageResponse getBySlug(String slug) {
        if (isBlank(slug)) {
            throw new PageServiceException(VALIDATION_ERROR, "slug is required.");
        }

        return pageDao.findBySlug(trim(slug))
                .map(this::toResponse)
                .orElseThrow(() -> new PageServiceException(PAGE_NOT_FOUND, "Page not found."));
    }

    public PageResponse getHomepage() {
        return pageDao.findHomepage()
                .map(this::toResponse)
                .orElseThrow(() -> new PageServiceException(PAGE_NOT_FOUND, "Homepage not found."));
    }

    public PageResponse createPage(CreatePageRequest request) {
        validateCreateRequest(request);

        String slug = trim(request.getSlug());
        ensureSlugAvailable(slug, null);

        Page page = new Page(
                null,
                trim(request.getTitle()),
                slug,
                trim(request.getContent()),
                request.getStatus(),
                trimToNull(request.getMetaTitle()),
                trimToNull(request.getMetaDescription()),
                request.getHomepage() == null ? Boolean.FALSE : request.getHomepage(),
                request.getMenuVisible() == null ? Boolean.TRUE : request.getMenuVisible()
        );

        Page createdPage = pageDao.create(page);
        normalizeHomepage(createdPage);
        LOGGER.info("Created page id={}, slug={}", createdPage.getId(), createdPage.getSlug());
        return toResponse(createdPage);
    }

    public PageResponse updatePage(Long id, UpdatePageRequest request) {
        validateUpdateRequest(request);

        Page page = loadPage(id);
        String slug = trim(request.getSlug());
        ensureSlugAvailable(slug, id);

        page.setTitle(trim(request.getTitle()));
        page.setSlug(slug);
        page.setContent(trim(request.getContent()));
        page.setStatus(request.getStatus());
        page.setMetaTitle(trimToNull(request.getMetaTitle()));
        page.setMetaDescription(trimToNull(request.getMetaDescription()));
        page.setHomepage(request.getHomepage() == null ? Boolean.FALSE : request.getHomepage());
        page.setMenuVisible(request.getMenuVisible() == null ? Boolean.TRUE : request.getMenuVisible());

        Page updatedPage = pageDao.update(page);
        normalizeHomepage(updatedPage);
        LOGGER.info("Updated page id={}, slug={}", updatedPage.getId(), updatedPage.getSlug());
        return toResponse(updatedPage);
    }

    public boolean deletePage(Long id) {
        Page page = loadPage(id);
        boolean deleted = pageDao.delete(page);
        LOGGER.info("Deleted page id={}, deleted={}", id, deleted);
        return deleted;
    }

    private Page loadPage(Long id) {
        if (id == null) {
            throw new PageServiceException(VALIDATION_ERROR, "Page id is required.");
        }

        return pageDao.findById(id)
                .orElseThrow(() -> new PageServiceException(PAGE_NOT_FOUND, "Page not found."));
    }

    private void validateCreateRequest(CreatePageRequest request) {
        if (request == null) {
            throw new PageServiceException(VALIDATION_ERROR, "Request body is required.");
        }

        validateCommonFields(request.getTitle(), request.getSlug(), request.getContent(), request.getStatus());
    }

    private void validateUpdateRequest(UpdatePageRequest request) {
        if (request == null) {
            throw new PageServiceException(VALIDATION_ERROR, "Request body is required.");
        }

        validateCommonFields(request.getTitle(), request.getSlug(), request.getContent(), request.getStatus());
    }

    private void validateCommonFields(String title, String slug, String content, PageStatus status) {
        if (isBlank(title)) {
            throw new PageServiceException(VALIDATION_ERROR, "title is required.");
        }
        if (isBlank(slug)) {
            throw new PageServiceException(VALIDATION_ERROR, "slug is required.");
        }
        if (isBlank(content)) {
            throw new PageServiceException(VALIDATION_ERROR, "content is required.");
        }
        if (status == null) {
            throw new PageServiceException(VALIDATION_ERROR, "status is required.");
        }
    }

    private void ensureSlugAvailable(String slug, Long currentPageId) {
        Optional<Page> existingPage = pageDao.findBySlug(slug);
        if (existingPage.isPresent() && !existingPage.get().getId().equals(currentPageId)) {
            throw new PageServiceException(DUPLICATE_SLUG, "slug is already used.");
        }
    }

    private void normalizeHomepage(Page selectedPage) {
        if (!Boolean.TRUE.equals(selectedPage.getHomepage())) {
            return;
        }

        List<Page> homepagePages = pageDao.findAll(QuerySpec.<PageProperty>create()
                .where(PageProperty.HOMEPAGE).equalsTo(Boolean.TRUE));
        for (Page page : homepagePages) {
            if (page.getId().equals(selectedPage.getId())) {
                continue;
            }
            page.setHomepage(Boolean.FALSE);
            pageDao.update(page);
        }
    }

    private PageResponse toResponse(Page page) {
        return new PageResponse(
                page.getId(),
                page.getTitle(),
                page.getSlug(),
                page.getContent(),
                page.getStatus(),
                page.getMetaTitle(),
                page.getMetaDescription(),
                page.getHomepage(),
                page.getMenuVisible(),
                toIsoString(page.getCreatedAt()),
                toIsoString(page.getUpdatedAt())
        );
    }

    private static String toIsoString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
