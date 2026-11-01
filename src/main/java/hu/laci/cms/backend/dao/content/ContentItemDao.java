package hu.laci.cms.backend.dao.content;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.content.ContentItem;
import hu.laci.cms.backend.model.content.ContentItemProperty;
import hu.laci.cms.backend.model.content.ContentStatus;
import hu.laci.cms.backend.model.content.ContentType;
import hu.laci.cms.backend.model.site.Site;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.sql.Timestamp;

/** Persistence contract for Site-owned content items and their tag assignments. */
public interface ContentItemDao extends CrudDao<ContentItem, ContentItemProperty> {
    long countBySite(Site site);
    List<ContentItem> findBySite(Site site, ContentType type, ContentStatus status, String groupCode);
    Optional<ContentItem> findBySiteAndId(Site site, Long id);
    Optional<ContentItem> findBySiteTypeAndSlug(Site site, ContentType type, String slug);
    long countByFeaturedMediaId(Long mediaId);
    void deleteTagAssignments(Long contentItemId);
    void replaceTagAssignments(Long contentItemId, Collection<Long> tagIds);
    List<Long> findTagIds(Long contentItemId);
    Map<Long, List<Long>> findTagIdsByContentItemIds(Collection<Long> contentItemIds);
    List<ContentItem> findPublishedPage(Site site, ContentType type, String groupCode, Timestamp publishedBefore, int limit, int offset);
    long countPublished(Site site, ContentType type, String groupCode, Timestamp publishedBefore);
    Optional<ContentItem> findPublicBySiteTypeAndSlug(Site site, ContentType type, String slug,
                                                       Timestamp publishedBefore);
}
