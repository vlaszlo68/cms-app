package hu.laci.cms.backend.service.content;

import hu.laci.cms.backend.dao.content.ContentCollectionDao;
import hu.laci.cms.backend.dao.content.ContentItemDao;
import hu.laci.cms.backend.dao.content.TagDao;
import hu.laci.cms.backend.dto.content.PublicArticleResponse;
import hu.laci.cms.backend.dto.content.PublicContentCollectionPageResponse;
import hu.laci.cms.backend.dto.content.PublicContentPreviewResponse;
import hu.laci.cms.backend.dto.content.PublicMediaResponse;
import hu.laci.cms.backend.model.content.ContentCollection;
import hu.laci.cms.backend.model.content.ContentItem;
import hu.laci.cms.backend.model.content.ContentStatus;
import hu.laci.cms.backend.model.content.ContentType;
import hu.laci.cms.backend.model.content.Tag;
import hu.laci.cms.backend.model.site.Site;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Applies the anonymous published-content visibility policy before creating public DTOs. */
public class PublicContentService {
    public static final String CONTENT_NOT_FOUND = "CONTENT_NOT_FOUND";
    public static final String COLLECTION_NOT_FOUND = "CONTENT_COLLECTION_NOT_FOUND";
    public static final String VALIDATION_ERROR = "INVALID_REQUEST";
    private final ContentItemDao itemDao;
    private final ContentCollectionDao collectionDao;
    private final TagDao tagDao;

    public PublicContentService(ContentItemDao itemDao, ContentCollectionDao collectionDao, TagDao tagDao) {
        this.itemDao = Objects.requireNonNull(itemDao); this.collectionDao = Objects.requireNonNull(collectionDao); this.tagDao = Objects.requireNonNull(tagDao);
    }
    public PublicContentCollectionPageResponse collection(Site site, String code, String pageValue, String pageSizeValue) {
        ContentCollection collection = collectionDao.findBySiteAndCode(site, normalizeCode(code)).filter(value -> Boolean.TRUE.equals(value.getActive()) && publicType(value.getContentType())).orElseThrow(() -> new ContentServiceException(COLLECTION_NOT_FOUND, "Content collection not found."));
        int page = parsePage(pageValue, 0); int pageSize = pageSizeValue == null ? collection.getPageSize() : parsePageSize(pageSizeValue);
        long offsetLong = (long) page * pageSize; if (offsetLong > Integer.MAX_VALUE) throw new ContentServiceException(VALIDATION_ERROR, "page is too large.");
        Timestamp now = Timestamp.from(Instant.now()); long total = itemDao.countPublished(site, collection.getContentType(), collection.getGroupCode(), now);
        List<ContentItem> items = itemDao.findPublishedPage(site, collection.getContentType(), collection.getGroupCode(), now, pageSize, (int) offsetLong);
        return new PublicContentCollectionPageResponse(previews(site, items), page, pageSize, total, total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize));
    }
    public PublicArticleResponse article(Site site, String typeValue, String slug) {
        ContentType type = parseType(typeValue); if (slug == null || slug.isBlank()) throw new ContentServiceException(CONTENT_NOT_FOUND, "Content not found.");
        ContentItem item = itemDao.findPublicBySiteTypeAndSlug(site, type, slug, Timestamp.from(Instant.now()))
                .orElseThrow(() -> new ContentServiceException(CONTENT_NOT_FOUND, "Content not found."));
        return new PublicArticleResponse(item.getType(), item.getSlug(), item.getTitle(), item.getSummary(), item.getContent(), item.getPublishedAt().toInstant().toString(), media(site, item), tags(site, List.of(item)).get(0), item.getGroupCode());
    }
    private List<PublicContentPreviewResponse> previews(Site site, List<ContentItem> items) { List<List<String>> tags = tags(site, items); return java.util.stream.IntStream.range(0, items.size()).mapToObj(index -> preview(items.get(index), tags.get(index))).toList(); }
    private PublicContentPreviewResponse preview(ContentItem item, List<String> tags) { return new PublicContentPreviewResponse(item.getType(), item.getSlug(), item.getTitle(), item.getSummary(), item.getPublishedAt().toInstant().toString(), media(item.getSite(), item), tags); }
    private static PublicMediaResponse media(Site site, ContentItem item) { if (item.getFeaturedMedia() == null || item.getFeaturedMedia().getId() == null) return null; return new PublicMediaResponse(item.getFeaturedMedia().getId(), "/api/public/sites/" + site.getCode() + "/media/" + item.getFeaturedMedia().getId() + "/content"); }
    private List<List<String>> tags(Site site, List<ContentItem> items) { Map<Long, Tag> tags = tagDao.findBySite(site).stream().collect(java.util.stream.Collectors.toMap(Tag::getId, value -> value)); Map<Long, List<Long>> ids = itemDao.findTagIdsByContentItemIds(items.stream().map(ContentItem::getId).toList()); return items.stream().map(item -> ids.getOrDefault(item.getId(), List.of()).stream().map(tags::get).filter(Objects::nonNull).map(Tag::getName).toList()).toList(); }
    private static boolean publicType(ContentType type) { return type == ContentType.NEWS || type == ContentType.BLOG; }
    private static ContentType parseType(String value) { if ("news".equals(value)) return ContentType.NEWS; if ("blog".equals(value)) return ContentType.BLOG; throw new ContentServiceException(CONTENT_NOT_FOUND, "Content not found."); }
    private static String normalizeCode(String value) { if (value == null || value.isBlank()) throw new ContentServiceException(VALIDATION_ERROR, "collectionCode is required."); return value.trim().toUpperCase(java.util.Locale.ROOT); }
    private static int parsePage(String value, int fallback) { if (value == null) return fallback; try { int page = Integer.parseInt(value); if (page < 0) throw new NumberFormatException(); return page; } catch (NumberFormatException exception) { throw new ContentServiceException(VALIDATION_ERROR, "page must be a non-negative integer."); } }
    private static int parsePageSize(String value) { int size = parsePage(value, 12); if (size < 1 || size > 100) throw new ContentServiceException(VALIDATION_ERROR, "pageSize must be between 1 and 100."); return size; }
}
