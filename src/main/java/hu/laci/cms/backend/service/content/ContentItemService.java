package hu.laci.cms.backend.service.content;

import hu.laci.cms.backend.dao.common.DataAccessException;
import hu.laci.cms.backend.dao.content.ContentItemDao;
import hu.laci.cms.backend.dao.content.TagDao;
import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dto.content.ContentItemRequest;
import hu.laci.cms.backend.dto.content.ContentItemResponse;
import hu.laci.cms.backend.dto.content.TagResponse;
import hu.laci.cms.backend.model.content.ContentItem;
import hu.laci.cms.backend.model.content.ContentStatus;
import hu.laci.cms.backend.model.content.ContentType;
import hu.laci.cms.backend.model.content.Tag;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.site.Site;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Business service for Site-isolated content CRUD and atomic tag assignment. */
public class ContentItemService {
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String CONTENT_ITEM_NOT_FOUND = "CONTENT_ITEM_NOT_FOUND";
    public static final String DUPLICATE_SLUG = "DUPLICATE_SLUG";
    public static final String TAG_NOT_FOUND = "TAG_NOT_FOUND";
    public static final String MEDIA_NOT_FOUND = "MEDIA_NOT_FOUND";
    private final ContentItemDao contentDao;
    private final TagDao tagDao;
    private final MediaDao mediaDao;

    /** Creates the service with persistence dependencies for Site validation. */
    public ContentItemService(ContentItemDao contentDao, TagDao tagDao, MediaDao mediaDao) { this.contentDao = Objects.requireNonNull(contentDao); this.tagDao = Objects.requireNonNull(tagDao); this.mediaDao = Objects.requireNonNull(mediaDao); }
    /** Lists Site-owned content, optionally filtered by exact type, status, and group code. */
    public List<ContentItemResponse> list(Site site, ContentType type, ContentStatus status, String groupCode) { requireSite(site); return toResponses(contentDao.findBySite(site, type, status, trimToNull(groupCode))); }
    /** Returns a content item only when owned by the supplied Site. */
    public ContentItemResponse get(Site site, Long id) { return toResponse(load(site, id)); }
    /** Creates content after all tag/media references are validated. */
    public ContentItemResponse create(Site site, ContentItemRequest request) { requireSite(site); validate(request); List<Long> tagIds = validatedTagIds(site, request.getTagIds()); ensureSlugAvailable(site, request.getType(), trim(request.getSlug()), null); try { ContentItem created = contentDao.create(apply(new ContentItem(), site, request, null)); contentDao.replaceTagAssignments(created.getId(), tagIds); return toResponse(created); } catch (DataAccessException exception) { throw translateDuplicate(exception); } }
    /** Updates content after all tag/media references are validated. */
    public ContentItemResponse update(Site site, Long id, ContentItemRequest request) { requireSite(site); validate(request); List<Long> tagIds = validatedTagIds(site, request.getTagIds()); ContentItem item = load(site, id); ensureSlugAvailable(site, request.getType(), trim(request.getSlug()), id); try { ContentItem updated = contentDao.update(apply(item, site, request, item.getPublishedAt())); contentDao.replaceTagAssignments(updated.getId(), tagIds); return toResponse(updated); } catch (DataAccessException exception) { throw translateDuplicate(exception); } }
    /** Deletes a Site-owned item and its join records without deleting reusable tags. */
    public boolean delete(Site site, Long id) { ContentItem item = load(site, id); contentDao.deleteTagAssignments(item.getId()); return contentDao.delete(item); }

    private List<ContentItemResponse> toResponses(List<ContentItem> items) { if (items.isEmpty()) { return List.of(); } Map<Long, List<Long>> tagIds = contentDao.findTagIdsByContentItemIds(items.stream().map(ContentItem::getId).toList()); Map<Long, Tag> tags = tagDao.findBySite(items.get(0).getSite()).stream().collect(Collectors.toMap(Tag::getId, Function.identity())); return items.stream().map(item -> toResponse(item, tagIds.getOrDefault(item.getId(), List.of()), tags)).toList(); }
    private ContentItem apply(ContentItem item, Site site, ContentItemRequest request, Timestamp existingPublishedAt) { item.setSite(site); item.setType(request.getType()); item.setTitle(trim(request.getTitle())); item.setSlug(trim(request.getSlug())); item.setSummary(trimToNull(request.getSummary())); item.setContent(trimToNull(request.getContent())); item.setGroupCode(trimToNull(request.getGroupCode())); item.setStatus(request.getStatus()); item.setFeaturedMedia(resolveMedia(site, request.getFeaturedMediaId())); item.setPublishedAt(resolvePublishedAt(request, existingPublishedAt)); return item; }
    private Timestamp resolvePublishedAt(ContentItemRequest request, Timestamp existingPublishedAt) { Timestamp supplied = parseInstant(request.getPublishedAt()); if (request.getStatus() == ContentStatus.PUBLISHED) { return supplied == null ? Timestamp.from(Instant.now()) : supplied; } return supplied == null ? existingPublishedAt : supplied; }
    private Media resolveMedia(Site site, Long id) { if (id == null) { return null; } return mediaDao.findBySiteAndId(site, id).orElseThrow(() -> new ContentServiceException(MEDIA_NOT_FOUND, "Featured media not found.")); }
    private List<Long> validatedTagIds(Site site, Collection<Long> ids) { if (ids == null) { return List.of(); } LinkedHashSet<Long> unique = new LinkedHashSet<>(); for (Long id : ids) { if (id == null || !unique.add(id)) { throw new ContentServiceException(VALIDATION_ERROR, "tagIds must contain unique IDs."); } if (tagDao.findBySiteAndId(site, id).isEmpty()) { throw new ContentServiceException(TAG_NOT_FOUND, "Tag not found."); } } return List.copyOf(unique); }
    private ContentItem load(Site site, Long id) { requireSite(site); if (id == null) { throw new ContentServiceException(VALIDATION_ERROR, "Content item id is required."); } return contentDao.findBySiteAndId(site, id).orElseThrow(() -> new ContentServiceException(CONTENT_ITEM_NOT_FOUND, "Content item not found.")); }
    private void validate(ContentItemRequest request) { if (request == null || request.getType() == null || request.getStatus() == null || blank(request.getTitle()) || blank(request.getSlug())) { throw new ContentServiceException(VALIDATION_ERROR, "type, title, slug and status are required."); } if (trim(request.getTitle()).length() > 255 || trim(request.getSlug()).length() > 100 || length(request.getSummary()) > 1000 || length(request.getGroupCode()) > 100) { throw new ContentServiceException(VALIDATION_ERROR, "One or more fields exceed their maximum length."); } if (!trim(request.getSlug()).matches("[a-z0-9]+(?:-[a-z0-9]+)*")) { throw new ContentServiceException(VALIDATION_ERROR, "slug must use lowercase URL-safe hyphenated text."); } parseInstant(request.getPublishedAt()); }
    private void ensureSlugAvailable(Site site, ContentType type, String slug, Long currentId) { Optional<ContentItem> existing = contentDao.findBySiteTypeAndSlug(site, type, slug); if (existing.isPresent() && !existing.get().getId().equals(currentId)) { throw new ContentServiceException(DUPLICATE_SLUG, "slug is already used for this content type."); } }
    private ContentItemResponse toResponse(ContentItem item) { return toResponses(List.of(item)).get(0); }
    private ContentItemResponse toResponse(ContentItem item, List<Long> tagIds, Map<Long, Tag> tags) { List<TagResponse> responseTags = tagIds.stream().map(tags::get).filter(Objects::nonNull).map(this::toTagResponse).toList(); return new ContentItemResponse(item.getId(), item.getType(), item.getTitle(), item.getSlug(), item.getSummary(), item.getContent(), item.getGroupCode(), item.getStatus(), item.getFeaturedMedia() == null ? null : item.getFeaturedMedia().getId(), iso(item.getPublishedAt()), responseTags, iso(item.getCreatedAt()), iso(item.getUpdatedAt())); }
    private TagResponse toTagResponse(Tag tag) { return new TagResponse(tag.getId(), tag.getCode(), tag.getName(), iso(tag.getCreatedAt()), iso(tag.getUpdatedAt())); }
    private static ContentServiceException translateDuplicate(DataAccessException exception) { if (isUniqueViolation(exception)) { return new ContentServiceException(DUPLICATE_SLUG, "slug is already used for this content type."); } throw exception; }
    private static boolean isUniqueViolation(Throwable exception) { for (Throwable current = exception; current != null; current = current.getCause()) { if (current instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) { return true; } } return false; }
    private static void requireSite(Site site) { if (site == null || site.getId() == null) { throw new ContentServiceException(VALIDATION_ERROR, "Site is required."); } }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String trimToNull(String value) { String trimmed = trim(value); return trimmed == null || trimmed.isEmpty() ? null : trimmed; }
    private static int length(String value) { return value == null ? 0 : trim(value).length(); }
    private static String iso(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant().toString(); }
    private static Timestamp parseInstant(String value) { if (value == null || value.isBlank()) { return null; } try { return Timestamp.from(Instant.parse(value)); } catch (DateTimeParseException exception) { throw new ContentServiceException(VALIDATION_ERROR, "publishedAt must be an ISO-8601 instant."); } }
}
