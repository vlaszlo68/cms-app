package hu.laci.cms.backend.dao.content;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dao.common.DataAccessException;
import hu.laci.cms.backend.dao.site.SiteDaoImpl;
import hu.laci.cms.backend.model.content.ContentCollection;
import hu.laci.cms.backend.model.content.ContentItem;
import hu.laci.cms.backend.model.content.ContentSort;
import hu.laci.cms.backend.model.content.ContentStatus;
import hu.laci.cms.backend.model.content.ContentType;
import hu.laci.cms.backend.model.content.Tag;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Database-backed contract tests for V21 Content Foundation persistence. */
class ContentFoundationDaoIntegrationTest {

    private static final String PREFIX = "content_foundation_test_";

    private ContentItemDao contentItemDao;
    private TagDao tagDao;
    private ContentCollectionDao collectionDao;
    private Site site;

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(emptyServletContext());
        DatabaseMigrationRunner.runMigrations();
    }

    @AfterAll
    static void shutdownDatabase() {
        DatabaseConfig.shutdown();
    }

    @BeforeEach
    void setUp() throws SQLException {
        SessionContext.clear();
        deleteTestData();
        contentItemDao = new ContentItemDaoImpl();
        tagDao = new TagDaoImpl();
        collectionDao = new ContentCollectionDaoImpl();
        site = new SiteDaoImpl().findByCode("default").orElseThrow();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        SessionContext.clear();
        deleteTestData();
    }

    @Test
    void persistsSiteScopedItemsTagsAssignmentsAndCollections() {
        Tag tag = new Tag();
        tag.setSite(site);
        tag.setCode("CONTENT_FOUNDATION_TEST_TAG");
        tag.setName("Content foundation test tag");
        Tag createdTag = tagDao.create(tag);

        ContentItem news = contentItem("news", ContentType.NEWS);
        ContentItem blog = contentItem("news", ContentType.BLOG);
        ContentItem createdNews = contentItemDao.create(news);
        ContentItem createdBlog = contentItemDao.create(blog);
        contentItemDao.replaceTagAssignments(createdNews.getId(), List.of(createdTag.getId()));

        ContentCollection collection = new ContentCollection();
        collection.setSite(site);
        collection.setCode("CONTENT_FOUNDATION_TEST_LATEST");
        collection.setName("Content foundation test latest");
        collection.setContentType(ContentType.NEWS);
        collection.setSort(ContentSort.PUBLISHED_DESC);
        collection.setPageSize(12);
        collection.setActive(Boolean.TRUE);
        ContentCollection createdCollection = collectionDao.create(collection);

        assertEquals(List.of(createdTag.getId()), contentItemDao.findTagIds(createdNews.getId()));
        assertTrue(contentItemDao.findBySiteTypeAndSlug(site, ContentType.NEWS, news.getSlug()).isPresent());
        assertTrue(contentItemDao.findBySiteTypeAndSlug(site, ContentType.BLOG, blog.getSlug()).isPresent());
        assertEquals(ContentSort.PUBLISHED_DESC, collectionDao.findBySiteAndId(site, createdCollection.getId())
                .orElseThrow().getSort());
        assertEquals(12, collectionDao.findBySiteAndId(site, createdCollection.getId()).orElseThrow().getPageSize());
    }

    @Test
    void databaseRejectsDuplicateSlugWithinSameSiteAndType() {
        ContentItem first = contentItem("duplicate", ContentType.NEWS);
        contentItemDao.create(first);
        assertThrows(DataAccessException.class, () -> contentItemDao.create(contentItem("duplicate", ContentType.NEWS)));
    }

    @Test
    void siteScopedLookupsDoNotExposeAnotherSitesContentOrTags() {
        Site otherSite = new Site(null, "content-foundation-other", "Content foundation other", true);
        Site createdOtherSite = new SiteDaoImpl().create(otherSite);
        ContentItem item = contentItem("isolated", ContentType.NEWS);
        ContentItem createdItem = contentItemDao.create(item);

        Tag tag = new Tag();
        tag.setSite(site);
        tag.setCode("CONTENT_FOUNDATION_TEST_ISOLATED");
        tag.setName("Isolated");
        Tag createdTag = tagDao.create(tag);

        assertTrue(contentItemDao.findBySiteAndId(createdOtherSite, createdItem.getId()).isEmpty());
        assertTrue(tagDao.findBySiteAndId(createdOtherSite, createdTag.getId()).isEmpty());
    }

    @Test
    void deletingContentRemovesItsTagAssignmentsAndPersistsInactiveCollection() {
        Tag tag = new Tag();
        tag.setSite(site);
        tag.setCode("CONTENT_FOUNDATION_TEST_CLEANUP");
        tag.setName("Cleanup");
        Tag createdTag = tagDao.create(tag);
        ContentItem createdItem = contentItemDao.create(contentItem("cleanup", ContentType.GALLERY));
        contentItemDao.replaceTagAssignments(createdItem.getId(), List.of(createdTag.getId()));

        ContentCollection collection = new ContentCollection();
        collection.setSite(site);
        collection.setCode("CONTENT_FOUNDATION_TEST_INACTIVE");
        collection.setName("Inactive");
        collection.setContentType(ContentType.GALLERY);
        collection.setSort(ContentSort.PUBLISHED_DESC);
        collection.setPageSize(1);
        collection.setActive(Boolean.FALSE);
        ContentCollection createdCollection = collectionDao.create(collection);

        assertTrue(contentItemDao.delete(createdItem));
        assertTrue(contentItemDao.findTagIds(createdItem.getId()).isEmpty());
        assertEquals(Boolean.FALSE, collectionDao.findBySiteAndId(site, createdCollection.getId())
                .orElseThrow().getActive());
    }

    @Test
    void publishedPageQueryExcludesDraftAndFutureContentAndKeepsStableOrder() {
        Timestamp now = Timestamp.from(Instant.parse("2026-11-01T12:00:00Z"));
        long baselineCount = contentItemDao.countPublished(site, ContentType.NEWS, null, now);
        ContentItem newest = contentItem("public-newest", ContentType.NEWS);
        newest.setStatus(ContentStatus.PUBLISHED);
        newest.setGroupCode("ENGINEERING");
        newest.setPublishedAt(Timestamp.from(Instant.parse("2026-11-01T11:00:00Z")));
        ContentItem older = contentItem("public-older", ContentType.NEWS);
        older.setStatus(ContentStatus.PUBLISHED);
        older.setGroupCode("ENGINEERING");
        older.setPublishedAt(Timestamp.from(Instant.parse("2026-11-01T10:00:00Z")));
        ContentItem draft = contentItem("public-draft", ContentType.NEWS);
        ContentItem future = contentItem("public-future", ContentType.NEWS);
        future.setStatus(ContentStatus.PUBLISHED);
        future.setPublishedAt(Timestamp.from(Instant.parse("2026-11-01T13:00:00Z")));
        contentItemDao.create(newest); contentItemDao.create(older); contentItemDao.create(draft); contentItemDao.create(future);

        List<ContentItem> page = contentItemDao.findPublishedPage(site, ContentType.NEWS, null, now, 12, 0);
        assertEquals(baselineCount + 2L, contentItemDao.countPublished(site, ContentType.NEWS, null, now));
        assertEquals(List.of(newest.getSlug(), older.getSlug()), page.stream().limit(2).map(ContentItem::getSlug).toList());
        assertFalse(page.stream().map(ContentItem::getSlug).anyMatch(draft.getSlug()::equals));
        assertFalse(page.stream().map(ContentItem::getSlug).anyMatch(future.getSlug()::equals));
    }

    @Test
    void publicDetailQueryDoesNotExposeDraftArchivedFutureWrongTypeOrOtherSite() {
        Timestamp now = Timestamp.from(Instant.parse("2026-11-01T12:00:00Z"));
        ContentItem published = contentItem("detail", ContentType.NEWS);
        published.setStatus(ContentStatus.PUBLISHED); published.setPublishedAt(Timestamp.from(Instant.parse("2026-11-01T11:00:00Z")));
        ContentItem draft = contentItem("detail-draft", ContentType.NEWS);
        ContentItem archived = contentItem("detail-archived", ContentType.NEWS);
        archived.setStatus(ContentStatus.ARCHIVED); archived.setPublishedAt(now);
        ContentItem future = contentItem("detail-future", ContentType.NEWS);
        future.setStatus(ContentStatus.PUBLISHED); future.setPublishedAt(Timestamp.from(Instant.parse("2026-11-01T13:00:00Z")));
        contentItemDao.create(published); contentItemDao.create(draft); contentItemDao.create(archived); contentItemDao.create(future);
        assertTrue(contentItemDao.findPublicBySiteTypeAndSlug(site, ContentType.NEWS, published.getSlug(), now).isPresent());
        assertTrue(contentItemDao.findPublicBySiteTypeAndSlug(site, ContentType.NEWS, draft.getSlug(), now).isEmpty());
        assertTrue(contentItemDao.findPublicBySiteTypeAndSlug(site, ContentType.NEWS, archived.getSlug(), now).isEmpty());
        assertTrue(contentItemDao.findPublicBySiteTypeAndSlug(site, ContentType.NEWS, future.getSlug(), now).isEmpty());
        assertTrue(contentItemDao.findPublicBySiteTypeAndSlug(site, ContentType.BLOG, published.getSlug(), now).isEmpty());
        Site otherSite = new SiteDaoImpl().create(new Site(null, "content-foundation-other", "Other", true));
        assertTrue(contentItemDao.findPublicBySiteTypeAndSlug(otherSite, ContentType.NEWS, published.getSlug(), now).isEmpty());
    }

    private ContentItem contentItem(String suffix, ContentType type) {
        ContentItem item = new ContentItem();
        item.setSite(site);
        item.setType(type);
        item.setTitle("Content foundation test " + suffix);
        item.setSlug(PREFIX + suffix);
        item.setStatus(ContentStatus.DRAFT);
        return item;
    }

    private void deleteTestData() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement collection = connection.prepareStatement(
                     "DELETE FROM content_collections WHERE code LIKE ?");
             PreparedStatement item = connection.prepareStatement(
                     "DELETE FROM content_items WHERE slug LIKE ?");
             PreparedStatement tag = connection.prepareStatement(
                     "DELETE FROM tags WHERE code LIKE ?");
             PreparedStatement otherSite = connection.prepareStatement(
                     "DELETE FROM sites WHERE code = ?")) {
            collection.setString(1, "CONTENT_FOUNDATION_TEST_%");
            collection.executeUpdate();
            item.setString(1, PREFIX + "%");
            item.executeUpdate();
            tag.setString(1, "CONTENT_FOUNDATION_TEST_%");
            tag.executeUpdate();
            otherSite.setString(1, "content-foundation-other");
            otherSite.executeUpdate();
        }
    }

    private static ServletContext emptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (proxy, method, arguments) -> {
                    if ("getInitParameter".equals(method.getName())) {
                        return null;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    return null;
                });
    }
}
