package hu.laci.cms.backend.service.page;

import hu.laci.cms.backend.dao.content.ContentCollectionDao;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dto.page.CreatePageBlockRequest;
import hu.laci.cms.backend.model.content.ContentCollection;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit coverage for the structured CONTENT_COLLECTION PageBlock boundary. */
class PageBlockServiceContentCollectionTest {
    @Test
    void acceptsSameSiteCollectionConfig() {
        Site site = site(); Page page = new Page(); page.setId(2L); ContentCollection collection = new ContentCollection(); collection.setId(3L); collection.setCode("JAVA_BLOGS");
        PageDao pages = Mockito.mock(PageDao.class); PageBlockDao blocks = Mockito.mock(PageBlockDao.class); ContentCollectionDao collections = Mockito.mock(ContentCollectionDao.class);
        Mockito.when(pages.findBySiteAndId(site, 2L)).thenReturn(Optional.of(page)); Mockito.when(collections.findBySiteAndCode(site, "JAVA_BLOGS")).thenReturn(Optional.of(collection)); Mockito.when(blocks.create(Mockito.any(PageBlock.class))).thenAnswer(invocation -> { PageBlock block = invocation.getArgument(0); block.setId(4L); return block; });
        PageBlockService service = new PageBlockService(pages, blocks, collections);
        assertEquals("CONTENT_COLLECTION", service.createBlock(site, 2L, new CreatePageBlockRequest(2L, "CONTENT_COLLECTION", null, 0, true, "{\"collectionCode\":\"JAVA_BLOGS\",\"displayMode\":\"CARDS\",\"paginationEnabled\":true}")).getBlockType());
    }
    @Test
    void rejectsMissingCollectionOrInvalidPresentationConfig() {
        Site site = site(); Page page = new Page(); page.setId(2L); PageDao pages = Mockito.mock(PageDao.class); PageBlockDao blocks = Mockito.mock(PageBlockDao.class); ContentCollectionDao collections = Mockito.mock(ContentCollectionDao.class);
        Mockito.when(pages.findBySiteAndId(site, 2L)).thenReturn(Optional.of(page));
        PageBlockService service = new PageBlockService(pages, blocks, collections);
        PageBlockServiceException error = assertThrows(PageBlockServiceException.class, () -> service.createBlock(site, 2L, new CreatePageBlockRequest(2L, "CONTENT_COLLECTION", null, 0, true, "{\"collectionCode\":\"MISSING\",\"displayMode\":\"INVALID\"}")));
        assertEquals(PageBlockService.VALIDATION_ERROR, error.getCode());
    }

    @Test
    void persistsDefaultPresentationValues() {
        Site site = site(); Page page = new Page(); page.setId(2L); ContentCollection collection = new ContentCollection(); collection.setCode("JAVA_BLOGS");
        PageDao pages = Mockito.mock(PageDao.class); PageBlockDao blocks = Mockito.mock(PageBlockDao.class); ContentCollectionDao collections = Mockito.mock(ContentCollectionDao.class);
        Mockito.when(pages.findBySiteAndId(site, 2L)).thenReturn(Optional.of(page)); Mockito.when(collections.findBySiteAndCode(site, "JAVA_BLOGS")).thenReturn(Optional.of(collection)); Mockito.when(blocks.create(Mockito.any(PageBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String config = new PageBlockService(pages, blocks, collections).createBlock(site, 2L, new CreatePageBlockRequest(2L, "CONTENT_COLLECTION", null, 0, true, "{\"collectionCode\":\"java_blogs\"}")).getConfigJson();
        assertEquals("{\"collectionCode\":\"JAVA_BLOGS\",\"displayMode\":\"CARDS\",\"paginationEnabled\":true}", config);
    }
    private static Site site() { Site site = new Site(); site.setId(1L); site.setCode("default"); return site; }
}
