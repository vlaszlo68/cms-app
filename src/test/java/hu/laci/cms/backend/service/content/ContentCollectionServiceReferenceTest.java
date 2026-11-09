package hu.laci.cms.backend.service.content;

import hu.laci.cms.backend.dao.content.ContentCollectionDao;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dto.content.ContentCollectionRequest;
import hu.laci.cms.backend.model.content.ContentCollection;
import hu.laci.cms.backend.model.content.ContentSort;
import hu.laci.cms.backend.model.content.ContentType;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regression tests for collection-code and PageBlock reference lifecycle safety. */
class ContentCollectionServiceReferenceTest {
    @Test
    void rejectsDeletingReferencedCollectionAndChangingItsCode() {
        Site site = new Site(1L, "default", "Default", true);
        ContentCollection collection = new ContentCollection(); collection.setId(2L); collection.setSite(site); collection.setCode("JAVA_BLOGS");
        PageBlock block = new PageBlock(); block.setBlockType("CONTENT_COLLECTION"); block.setConfigJson("{\"collectionCode\":\"JAVA_BLOGS\"}");
        ContentCollectionDao collections = Mockito.mock(ContentCollectionDao.class);
        PageBlockDao blocks = Mockito.mock(PageBlockDao.class);
        Mockito.when(collections.findBySiteAndId(site, 2L)).thenReturn(Optional.of(collection));
        Mockito.when(blocks.findBySite(site)).thenReturn(List.of(block));
        ContentCollectionService service = new ContentCollectionService(collections, blocks);
        ContentServiceException delete = assertThrows(ContentServiceException.class, () -> service.delete(site, 2L));
        assertEquals(ContentCollectionService.COLLECTION_IN_USE, delete.getCode());
        ContentCollectionRequest renamed = new ContentCollectionRequest();
        renamed.setCode("OTHER");
        renamed.setName("Java");
        renamed.setContentType(ContentType.BLOG);
        renamed.setSort(ContentSort.PUBLISHED_DESC);
        renamed.setPageSize(12);
        renamed.setActive(true);
        assertThrows(ContentServiceException.class, () -> service.update(site, 2L, renamed));
    }
}
