package hu.laci.cms.backend.service.site;

import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dto.site.CreateSiteRequest;
import hu.laci.cms.backend.dto.site.SiteRequestBase;
import hu.laci.cms.backend.dto.site.SiteResponse;
import hu.laci.cms.backend.dto.site.UpdateSiteRequest;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.site.SiteProperty;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Manages global Site records and resolves explicit Site context for owned operations.
 */
public class SiteService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String SITE_NOT_FOUND = "SITE_NOT_FOUND";
    public static final String DUPLICATE_SITE_CODE = "DUPLICATE_SITE_CODE";
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final SiteDao siteDao;

    /**
     * Creates the Site business service.
     *
     * @param siteDao Site persistence dependency
     */
    public SiteService(SiteDao siteDao) {
        this.siteDao = Objects.requireNonNull(siteDao, "siteDao must not be null");
    }

    /**
     * Lists all Sites, including inactive Sites, for administration.
     *
     * @return Sites ordered by name
     */
    public List<SiteResponse> listSites() {
        return siteDao.findAll(QuerySpec.<SiteProperty>create().orderBy(SiteProperty.NAME.asc()))
                .stream().map(this::toResponse).toList();
    }

    /**
     * Returns one Site for administration.
     *
     * @param code stable Site code
     * @return Site response
     * @throws SiteServiceException when the code is invalid or the Site is missing
     */
    public SiteResponse getSite(String code) {
        return toResponse(requireSite(code));
    }

    /**
     * Resolves a Site without applying its public active-state rule.
     *
     * @param code stable Site code
     * @return persisted Site
     * @throws SiteServiceException when the code is invalid or the Site is missing
     */
    public Site requireSite(String code) {
        String normalizedCode = validateCodeValue(code);
        return siteDao.findByCode(normalizedCode)
                .orElseThrow(() -> new SiteServiceException(SITE_NOT_FOUND, "Site not found."));
    }

    /**
     * Resolves a Site that is eligible for anonymous public access.
     *
     * @param code stable Site code
     * @return active persisted Site
     * @throws SiteServiceException when the Site is missing or inactive
     */
    public Site requireActiveSite(String code) {
        Site site = requireSite(code);
        if (!site.isActive()) {
            throw new SiteServiceException(SITE_NOT_FOUND, "Site not found.");
        }
        return site;
    }

    /**
     * Creates a globally identifiable Site.
     *
     * @param request editable Site fields
     * @return created Site response
     * @throws SiteServiceException when validation or code uniqueness fails
     */
    public SiteResponse createSite(CreateSiteRequest request) {
        validate(request);
        String code = request.getCode().trim();
        ensureCodeAvailable(code, null);
        Site site = new Site(null, code, request.getName().trim(), request.getActive() == null || request.getActive(),
                resolveThemeCode(request.getThemeCode()));
        return toResponse(siteDao.create(site));
    }

    /**
     * Updates the Site identified by its current stable code.
     *
     * @param currentCode current path code
     * @param request replacement editable fields
     * @return updated Site response
     * @throws SiteServiceException when the Site is missing or validation or uniqueness fails
     */
    public SiteResponse updateSite(String currentCode, UpdateSiteRequest request) {
        validate(request);
        Site site = requireSite(currentCode);
        String code = request.getCode().trim();
        ensureCodeAvailable(code, site.getId());
        site.setCode(code);
        site.setName(request.getName().trim());
        site.setActive(request.getActive() == null || request.getActive());
        site.setThemeCode(resolveThemeCode(request.getThemeCode()));
        return toResponse(siteDao.update(site));
    }

    private void ensureCodeAvailable(String code, Long currentId) {
        Optional<Site> existing = siteDao.findByCode(code);
        if (existing.isPresent() && !existing.get().getId().equals(currentId)) {
            throw new SiteServiceException(DUPLICATE_SITE_CODE, "code is already used.");
        }
    }

    private static void validate(SiteRequestBase request) {
        if (request == null) {
            throw new SiteServiceException(VALIDATION_ERROR, "Request body is required.");
        }
        validateCodeValue(request.getCode());
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new SiteServiceException(VALIDATION_ERROR, "name is required.");
        }
        resolveThemeCode(request.getThemeCode());
    }

    private static String validateCodeValue(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new SiteServiceException(VALIDATION_ERROR, "code is required.");
        }
        String normalized = code.trim();
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new SiteServiceException(VALIDATION_ERROR,
                    "code must contain lowercase letters, digits, and single hyphens only.");
        }
        return normalized;
    }

    private SiteResponse toResponse(Site site) {
        return new SiteResponse(site.getId(), site.getCode(), site.getName(), site.isActive(),
                site.getThemeCode(), toIsoString(site.getCreatedAt()), toIsoString(site.getUpdatedAt()));
    }

    private static String resolveThemeCode(String themeCode) {
        String normalized = themeCode == null || themeCode.trim().isEmpty()
                ? ThemeDefinitionRegistry.STANDARD : themeCode.trim();
        if (ThemeDefinitionRegistry.find(normalized).isEmpty()) {
            throw new SiteServiceException(VALIDATION_ERROR, "themeCode is not supported.");
        }
        return normalized;
    }

    private static String toIsoString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
