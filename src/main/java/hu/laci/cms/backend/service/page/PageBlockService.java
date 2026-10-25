package hu.laci.cms.backend.service.page;

import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dto.page.CreatePageBlockRequest;
import hu.laci.cms.backend.dto.page.PageBlockRequestBase;
import hu.laci.cms.backend.dto.page.PageBlockResponse;
import hu.laci.cms.backend.dto.page.UpdatePageBlockRequest;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.site.Site;

import java.util.List;
import java.util.Objects;

/**
 * Business service for ordered page block configuration management.
 */
public class PageBlockService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String PAGE_NOT_FOUND = "PAGE_NOT_FOUND";
    public static final String PAGE_BLOCK_NOT_FOUND = "PAGE_BLOCK_NOT_FOUND";

    private final PageDao pageDao;
    private final PageBlockDao pageBlockDao;

    public PageBlockService(PageDao pageDao, PageBlockDao pageBlockDao) {
        this.pageDao = Objects.requireNonNull(pageDao, "pageDao must not be null");
        this.pageBlockDao = Objects.requireNonNull(pageBlockDao, "pageBlockDao must not be null");
    }

    public List<PageBlockResponse> listBlocks(Site site, Long pageId) {
        requirePage(site, pageId);
        return pageBlockDao.findByPageId(pageId).stream().map(this::toResponse).toList();
    }

    public List<PageBlockResponse> listVisibleBlocks(Site site, Long pageId) {
        requirePage(site, pageId);
        return pageBlockDao.findVisibleByPageId(pageId).stream().map(this::toResponse).toList();
    }

    public PageBlockResponse getBlock(Site site, Long pageId, Long id) {
        requirePage(site, pageId);
        return toResponse(loadBlock(pageId, id));
    }

    public PageBlockResponse createBlock(Site site, Long pageId, CreatePageBlockRequest request) {
        validate(request);
        requirePathPage(request.getPageId(), pageId);
        requirePage(site, pageId);
        PageBlock block = new PageBlock(null, pageId, request.getBlockType().trim(),
                trimToNull(request.getTitle()), defaultOrder(request.getSortOrder()),
                request.getVisible() == null || request.getVisible(), trimToNull(request.getConfigJson()));
        return toResponse(pageBlockDao.create(block));
    }

    public PageBlockResponse updateBlock(Site site, Long pageId, Long id, UpdatePageBlockRequest request) {
        validate(request);
        requirePathPage(request.getPageId(), pageId);
        requirePage(site, pageId);
        PageBlock block = loadBlock(pageId, id);
        block.setPageId(pageId);
        block.setBlockType(request.getBlockType().trim());
        block.setTitle(trimToNull(request.getTitle()));
        block.setSortOrder(defaultOrder(request.getSortOrder()));
        block.setVisible(request.getVisible() == null || request.getVisible());
        block.setConfigJson(trimToNull(request.getConfigJson()));
        return toResponse(pageBlockDao.update(block));
    }

    public boolean deleteBlock(Site site, Long pageId, Long id) {
        requirePage(site, pageId);
        return pageBlockDao.delete(loadBlock(pageId, id));
    }

    private PageBlock loadBlock(Long pageId, Long id) {
        if (id == null) {
            throw new PageBlockServiceException(VALIDATION_ERROR, "Page block id is required.");
        }
        PageBlock block = pageBlockDao.findById(id)
                .orElseThrow(() -> new PageBlockServiceException(PAGE_BLOCK_NOT_FOUND, "Page block not found."));
        if (!pageId.equals(block.getPageId())) {
            throw new PageBlockServiceException(PAGE_BLOCK_NOT_FOUND, "Page block not found.");
        }
        return block;
    }

    private void requirePage(Site site, Long pageId) {
        if (pageId == null) {
            throw new PageBlockServiceException(VALIDATION_ERROR, "pageId is required.");
        }
        if (site == null || site.getId() == null || pageDao.findBySiteAndId(site, pageId).isEmpty()) {
            throw new PageBlockServiceException(PAGE_NOT_FOUND, "Page not found.");
        }
    }

    private static void requirePathPage(Long requestPageId, Long pathPageId) {
        if (pathPageId == null || requestPageId == null || !pathPageId.equals(requestPageId)) {
            throw new PageBlockServiceException(VALIDATION_ERROR, "pageId must match the page path.");
        }
    }

    private static void validate(PageBlockRequestBase request) {
        if (request == null) {
            throw new PageBlockServiceException(VALIDATION_ERROR, "Request body is required.");
        }
        if (request.getPageId() == null) {
            throw new PageBlockServiceException(VALIDATION_ERROR, "pageId is required.");
        }
        if (request.getBlockType() == null || request.getBlockType().isBlank()) {
            throw new PageBlockServiceException(VALIDATION_ERROR, "blockType is required.");
        }
    }

    private PageBlockResponse toResponse(PageBlock block) {
        return new PageBlockResponse(block.getId(), block.getPageId(), block.getBlockType(), block.getTitle(),
                block.getSortOrder(), block.isVisible(), block.getConfigJson());
    }

    private static int defaultOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
