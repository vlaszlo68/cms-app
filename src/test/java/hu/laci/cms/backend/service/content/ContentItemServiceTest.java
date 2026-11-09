package hu.laci.cms.backend.service.content;

import hu.laci.cms.backend.dao.content.ContentItemDao;
import hu.laci.cms.backend.dao.content.TagDao;
import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dto.content.ContentItemRequest;
import hu.laci.cms.backend.model.content.ContentItem;
import hu.laci.cms.backend.model.content.ContentStatus;
import hu.laci.cms.backend.model.content.ContentType;
import hu.laci.cms.backend.model.content.Tag;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Focused unit coverage for Content Foundation lifecycle and isolation rules. */
class ContentItemServiceTest {
    private static final Site SITE = new Site(1L, "content-test", "Content test", true);

    @Test
    void archiveWithoutTimestampPreservesPublicationHistory() {
        ContentItemDao contentDao = Mockito.mock(ContentItemDao.class);
        TagDao tagDao = Mockito.mock(TagDao.class);
        MediaDao mediaDao = Mockito.mock(MediaDao.class);
        ContentItem existing = item(7L, ContentStatus.PUBLISHED, Timestamp.from(Instant.parse("2026-10-01T00:00:00Z")));
        Mockito.when(contentDao.findBySiteAndId(SITE, 7L)).thenReturn(Optional.of(existing));
        Mockito.when(contentDao.findBySiteTypeAndSlug(SITE, ContentType.NEWS, "launch")).thenReturn(Optional.of(existing));
        Mockito.when(contentDao.update(existing)).thenReturn(existing);
        Mockito.when(contentDao.findTagIdsByContentItemIds(List.of(7L))).thenReturn(java.util.Map.of());
        Mockito.when(tagDao.findBySite(SITE)).thenReturn(List.of());
        ContentItemRequest request = request(ContentStatus.ARCHIVED);
        new ContentItemService(contentDao, tagDao, mediaDao).update(SITE, 7L, request);
        assertEquals("2026-10-01T00:00:00Z", existing.getPublishedAt().toInstant().toString());
    }

    @Test
    void crossSiteTagIsRejectedBeforeContentMutation() {
        ContentItemDao contentDao = Mockito.mock(ContentItemDao.class);
        TagDao tagDao = Mockito.mock(TagDao.class);
        Mockito.when(contentDao.findBySiteTypeAndSlug(SITE, ContentType.NEWS, "launch")).thenReturn(Optional.empty());
        Mockito.when(tagDao.findBySiteAndId(SITE, 88L)).thenReturn(Optional.empty());
        ContentItemRequest request = request(ContentStatus.DRAFT);
        request.setTagIds(List.of(88L));
        ContentServiceException exception = assertThrows(ContentServiceException.class,
                () -> new ContentItemService(contentDao, tagDao, Mockito.mock(MediaDao.class)).create(SITE, request));
        assertEquals(ContentItemService.TAG_NOT_FOUND, exception.getCode());
        Mockito.verify(contentDao, Mockito.never()).create(Mockito.any());
    }

    @Test
    void contentTypesShareSlugButDuplicateWithinTypeIsRejected() {
        ContentItemDao contentDao = Mockito.mock(ContentItemDao.class);
        ContentItem existing = item(8L, ContentStatus.DRAFT, null);
        Mockito.when(contentDao.findBySiteTypeAndSlug(SITE, ContentType.NEWS, "launch")).thenReturn(Optional.of(existing));
        ContentServiceException exception = assertThrows(ContentServiceException.class,
                () -> new ContentItemService(contentDao, Mockito.mock(TagDao.class), Mockito.mock(MediaDao.class)).create(SITE, request(ContentStatus.DRAFT)));
        assertEquals(ContentItemService.DUPLICATE_SLUG, exception.getCode());
    }

    private static ContentItem item(Long id, ContentStatus status, Timestamp publishedAt) { ContentItem item = new ContentItem(); item.setId(id); item.setSite(SITE); item.setType(ContentType.NEWS); item.setTitle("Launch"); item.setSlug("launch"); item.setStatus(status); item.setPublishedAt(publishedAt); return item; }
    private static ContentItemRequest request(ContentStatus status) { ContentItemRequest request = new ContentItemRequest(); request.setType(ContentType.NEWS); request.setTitle("Launch"); request.setSlug("launch"); request.setStatus(status); return request; }
}
