package hu.laci.cms.backend.service.content;

import hu.laci.cms.backend.dao.common.DataAccessException;
import hu.laci.cms.backend.dao.content.ContentCollectionDao;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.model.page.PageBlock;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.laci.cms.backend.dto.content.ContentCollectionRequest;
import hu.laci.cms.backend.dto.content.ContentCollectionResponse;
import hu.laci.cms.backend.model.content.ContentCollection;
import hu.laci.cms.backend.model.content.ContentSort;
import hu.laci.cms.backend.model.site.Site;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Business service for Site-isolated saved content collection CRUD. */
public class ContentCollectionService {
    public static final String COLLECTION_NOT_FOUND = "CONTENT_COLLECTION_NOT_FOUND";
    public static final String DUPLICATE_CODE = "DUPLICATE_CONTENT_COLLECTION_CODE";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String COLLECTION_IN_USE = "CONTENT_COLLECTION_IN_USE";
    private final ContentCollectionDao dao;
    private final PageBlockDao pageBlockDao;
    /** Creates the collection service. */
    public ContentCollectionService(ContentCollectionDao dao, PageBlockDao pageBlockDao) { this.dao = Objects.requireNonNull(dao); this.pageBlockDao = Objects.requireNonNull(pageBlockDao); }
    /** Lists saved query definitions owned by the Site. */
    public List<ContentCollectionResponse> list(Site site) { requireSite(site); return dao.findBySite(site).stream().map(this::response).toList(); }
    /** Gets a Site-owned collection. */
    public ContentCollectionResponse get(Site site, Long id) { return response(load(site, id)); }
    /** Creates a saved query definition without stored memberships. */
    public ContentCollectionResponse create(Site site, ContentCollectionRequest request) { requireSite(site); validate(request); String code = code(request.getCode()); if (dao.findBySiteAndCode(site, code).isPresent()) { throw duplicate(); } try { return response(dao.create(apply(new ContentCollection(), site, request, code))); } catch (DataAccessException exception) { throw translateDuplicate(exception); } }
    /** Updates a Site-owned saved query definition. */
    public ContentCollectionResponse update(Site site, Long id, ContentCollectionRequest request) { validate(request); ContentCollection collection = load(site, id); String code = code(request.getCode()); if (!collection.getCode().equals(code)) throw new ContentServiceException(VALIDATION_ERROR, "Collection code is immutable."); try { return response(dao.update(apply(collection, site, request, code))); } catch (DataAccessException exception) { throw translateDuplicate(exception); } }
    /** Deletes a Site-owned collection. */
    public boolean delete(Site site, Long id) { ContentCollection collection = load(site, id); if (pageBlockDao.findBySite(site).stream().anyMatch(block -> references(block, collection.getCode()))) throw new ContentServiceException(COLLECTION_IN_USE, "Content collection is referenced by a page block."); return dao.delete(collection); }
    private static boolean references(PageBlock block, String code) { if (!"CONTENT_COLLECTION".equals(block.getBlockType()) || block.getConfigJson() == null) return false; try { JsonObject config = JsonParser.parseString(block.getConfigJson()).getAsJsonObject(); return config.has("collectionCode") && code.equals(config.get("collectionCode").getAsString().trim().toUpperCase(Locale.ROOT)); } catch (RuntimeException ignored) { return false; } }
    private ContentCollection apply(ContentCollection collection, Site site, ContentCollectionRequest request, String code) { collection.setSite(site); collection.setCode(code); collection.setName(request.getName().trim()); collection.setContentType(request.getContentType()); collection.setGroupCode(nullIfBlank(request.getGroupCode())); collection.setSort(request.getSort() == null ? ContentSort.PUBLISHED_DESC : request.getSort()); collection.setPageSize(request.getPageSize() == null ? 12 : request.getPageSize()); collection.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive()); return collection; }
    private ContentCollection load(Site site, Long id) { requireSite(site); if (id == null) { throw new ContentServiceException(VALIDATION_ERROR, "Collection id is required."); } return dao.findBySiteAndId(site, id).orElseThrow(() -> new ContentServiceException(COLLECTION_NOT_FOUND, "Content collection not found.")); }
    private static void validate(ContentCollectionRequest request) { if (request == null || blank(request.getCode()) || blank(request.getName()) || request.getContentType() == null || request.getCode().trim().length() > 100 || request.getName().trim().length() > 255 || length(request.getGroupCode()) > 100 || !code(request.getCode()).matches("[A-Z0-9]+(?:[-_][A-Z0-9]+)*") || (request.getSort() != null && request.getSort() != ContentSort.PUBLISHED_DESC) || (request.getPageSize() != null && (request.getPageSize() < 1 || request.getPageSize() > 100))) { throw new ContentServiceException(VALIDATION_ERROR, "Invalid content collection request."); } }
    private static String code(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static int length(String value) { return value == null ? 0 : value.trim().length(); }
    private static String nullIfBlank(String value) { return blank(value) ? null : value.trim(); }
    private static void requireSite(Site site) { if (site == null || site.getId() == null) { throw new ContentServiceException(VALIDATION_ERROR, "Site is required."); } }
    private static ContentServiceException duplicate() { return new ContentServiceException(DUPLICATE_CODE, "code is already used."); }
    private static ContentServiceException translateDuplicate(DataAccessException exception) { for (Throwable current = exception; current != null; current = current.getCause()) { if (current instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) { return duplicate(); } } throw exception; }
    private ContentCollectionResponse response(ContentCollection collection) { return new ContentCollectionResponse(collection.getId(), collection.getCode(), collection.getName(), collection.getContentType(), collection.getGroupCode(), collection.getSort(), collection.getPageSize(), collection.getActive(), iso(collection.getCreatedAt()), iso(collection.getUpdatedAt())); }
    private static String iso(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant().toString(); }
}
