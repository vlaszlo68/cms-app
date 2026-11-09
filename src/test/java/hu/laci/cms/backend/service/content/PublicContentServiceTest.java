package hu.laci.cms.backend.service.content;

import hu.laci.cms.backend.dao.content.ContentCollectionDao;
import hu.laci.cms.backend.dao.content.ContentItemDao;
import hu.laci.cms.backend.dao.content.TagDao;
import hu.laci.cms.backend.dto.content.PublicContentCollectionPageResponse;
import hu.laci.cms.backend.model.content.ContentCollection;
import hu.laci.cms.backend.model.content.ContentStatus;
import hu.laci.cms.backend.model.content.ContentType;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Focused public visibility and pagination validation coverage. */
class PublicContentServiceTest {
    @Test
    void inactiveAndGalleryCollectionsAreNotPublic() {
        Site site = site();
        ContentCollection inactive = collection(ContentType.NEWS, false);
        ContentCollection gallery = collection(ContentType.GALLERY, true);
        ContentCollectionDao collections = Mockito.mock(ContentCollectionDao.class);
        Mockito.when(collections.findBySiteAndCode(site, "LATEST")).thenReturn(Optional.of(inactive));
        PublicContentService service = service(collections);
        assertThrows(ContentServiceException.class, () -> service.collection(site, "LATEST", null, null));
        Mockito.when(collections.findBySiteAndCode(site, "LATEST")).thenReturn(Optional.of(gallery));
        assertThrows(ContentServiceException.class, () -> service.collection(site, "LATEST", null, null));
    }

    @Test
    void validatesPageAndReturnsTruthfulEmptyPageTotals() {
        Site site = site();
        ContentCollectionDao collections = Mockito.mock(ContentCollectionDao.class);
        ContentItemDao items = Mockito.mock(ContentItemDao.class);
        Mockito.when(collections.findBySiteAndCode(site, "LATEST")).thenReturn(Optional.of(collection(ContentType.NEWS, true)));
        Mockito.when(items.countPublished(Mockito.eq(site), Mockito.eq(ContentType.NEWS), Mockito.isNull(), Mockito.any()))
                .thenReturn(13L);
        Mockito.when(items.findPublishedPage(Mockito.eq(site), Mockito.eq(ContentType.NEWS), Mockito.isNull(), Mockito.any(), Mockito.eq(12), Mockito.eq(24)))
                .thenReturn(List.of());
        PublicContentService service = new PublicContentService(items, collections, Mockito.mock(TagDao.class));
        PublicContentCollectionPageResponse page = service.collection(site, "LATEST", "2", null);
        assertEquals(13L, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(List.of(), page.items());
        assertThrows(ContentServiceException.class, () -> service.collection(site, "LATEST", "-1", null));
        assertThrows(ContentServiceException.class, () -> service.collection(site, "LATEST", "0", "101"));
    }

    private static PublicContentService service(ContentCollectionDao collections) {
        return new PublicContentService(Mockito.mock(ContentItemDao.class), collections, Mockito.mock(TagDao.class));
    }

    private static Site site() { Site site = new Site(); site.setId(1L); site.setCode("default"); return site; }
    private static ContentCollection collection(ContentType type, boolean active) {
        ContentCollection value = new ContentCollection(); value.setContentType(type); value.setActive(active); value.setPageSize(12); return value;
    }
}
