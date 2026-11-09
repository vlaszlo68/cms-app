package hu.laci.cms.backend.service.content;

import hu.laci.cms.backend.dao.common.DataAccessException;
import hu.laci.cms.backend.dao.content.TagDao;
import hu.laci.cms.backend.dto.content.TagRequest;
import hu.laci.cms.backend.dto.content.TagResponse;
import hu.laci.cms.backend.model.content.Tag;
import hu.laci.cms.backend.model.site.Site;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Business service for Site-isolated tag CRUD and usage protection. */
public class TagService {
    public static final String TAG_NOT_FOUND = "TAG_NOT_FOUND";
    public static final String DUPLICATE_CODE = "DUPLICATE_TAG_CODE";
    public static final String TAG_IN_USE = "TAG_IN_USE";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private final TagDao dao;
    /** Creates the tag service. */
    public TagService(TagDao dao) { this.dao = Objects.requireNonNull(dao); }
    /** Lists tags for the Site. */
    public List<TagResponse> list(Site site) { requireSite(site); return dao.findBySite(site).stream().map(this::response).toList(); }
    /** Gets a tag only if it belongs to the Site. */
    public TagResponse get(Site site, Long id) { return response(load(site, id)); }
    /** Creates a normalized Site tag. */
    public TagResponse create(Site site, TagRequest request) { requireSite(site); validate(request); String code = code(request.getCode()); if (dao.findBySiteAndCode(site, code).isPresent()) { throw duplicate(); } Tag tag = new Tag(); tag.setSite(site); tag.setCode(code); tag.setName(request.getName().trim()); try { return response(dao.create(tag)); } catch (DataAccessException exception) { throw translateDuplicate(exception); } }
    /** Updates a Site tag when it is unambiguous. */
    public TagResponse update(Site site, Long id, TagRequest request) { validate(request); Tag tag = load(site, id); String code = code(request.getCode()); Optional<Tag> existing = dao.findBySiteAndCode(site, code); if (existing.isPresent() && !existing.get().getId().equals(id)) { throw duplicate(); } tag.setCode(code); tag.setName(request.getName().trim()); try { return response(dao.update(tag)); } catch (DataAccessException exception) { throw translateDuplicate(exception); } }
    /** Deletes an unused tag. */
    public boolean delete(Site site, Long id) { Tag tag = load(site, id); if (dao.countContentItems(tag) > 0) { throw new ContentServiceException(TAG_IN_USE, "Tag is assigned to content items."); } return dao.delete(tag); }
    private Tag load(Site site, Long id) { requireSite(site); if (id == null) { throw new ContentServiceException(VALIDATION_ERROR, "Tag id is required."); } return dao.findBySiteAndId(site, id).orElseThrow(() -> new ContentServiceException(TAG_NOT_FOUND, "Tag not found.")); }
    private static void validate(TagRequest request) { if (request == null || blank(request.getCode()) || blank(request.getName()) || request.getCode().trim().length() > 100 || request.getName().trim().length() > 255 || !code(request.getCode()).matches("[A-Z0-9]+(?:[-_][A-Z0-9]+)*")) { throw new ContentServiceException(VALIDATION_ERROR, "code and name are required and must be valid."); } }
    private static String code(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static ContentServiceException duplicate() { return new ContentServiceException(DUPLICATE_CODE, "code is already used."); }
    private static ContentServiceException translateDuplicate(DataAccessException exception) { for (Throwable current = exception; current != null; current = current.getCause()) { if (current instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) { return duplicate(); } } throw exception; }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static void requireSite(Site site) { if (site == null || site.getId() == null) { throw new ContentServiceException(VALIDATION_ERROR, "Site is required."); } }
    private TagResponse response(Tag tag) { return new TagResponse(tag.getId(), tag.getCode(), tag.getName(), iso(tag.getCreatedAt()), iso(tag.getUpdatedAt())); }
    private static String iso(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant().toString(); }
}
