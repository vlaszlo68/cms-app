package hu.laci.cms.backend.service.settings;

import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dto.settings.SaveSiteSettingsRequest;
import hu.laci.cms.backend.dto.settings.PublicSiteSettingsResponse;
import hu.laci.cms.backend.dto.settings.SiteSettingsResponse;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.site.Site;

import java.util.Objects;

/**
 * Reads and updates the one settings record owned by a Site.
 */
public class SiteSettingsService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_LOGO_MEDIA = "INVALID_LOGO_MEDIA";

    private final SiteSettingsDao siteSettingsDao;
    private final MediaDao mediaDao;

    public SiteSettingsService(SiteSettingsDao siteSettingsDao, MediaDao mediaDao) {
        this.siteSettingsDao = Objects.requireNonNull(siteSettingsDao, "siteSettingsDao must not be null");
        this.mediaDao = Objects.requireNonNull(mediaDao, "mediaDao must not be null");
    }

    public SiteSettingsResponse getSettings(Site site) {
        return toResponse(loadOrCreateSettings(requireSite(site)));
    }

    public SiteSettingsResponse saveSettings(Site site, SaveSiteSettingsRequest request) {
        requireSite(site);
        if (request == null) {
            throw new SiteSettingsServiceException(VALIDATION_ERROR, "Request body is required.");
        }

        if (request.getLogoMediaId() != null && mediaDao.findBySiteAndId(site, request.getLogoMediaId()).isEmpty()) {
            throw new SiteSettingsServiceException(INVALID_LOGO_MEDIA,
                    "Logo media must belong to the requested Site.");
        }
        SiteSettings settings = loadOrCreateSettings(site);
        settings.setSiteName(trimToNull(request.getSiteName()));
        settings.setLogoMediaId(request.getLogoMediaId());
        settings.setFooterText(trimToNull(request.getFooterText()));
        settings.setContactEmail(trimToNull(request.getContactEmail()));
        settings.setPhone(trimToNull(request.getPhone()));
        settings.setFacebookUrl(trimToNull(request.getFacebookUrl()));
        settings.setLinkedinUrl(trimToNull(request.getLinkedinUrl()));
        return toResponse(siteSettingsDao.update(settings));
    }

    /**
     * Returns the anonymous public settings contract without creating the singleton record.
     *
     * @return public settings, or an all-null field fallback when no record exists
     */
    public PublicSiteSettingsResponse getPublicSettings(Site site) {
        requireSite(site);
        return siteSettingsDao.findBySite(site)
                .map(settings -> toPublicResponse(site, settings))
                .orElseGet(() -> new PublicSiteSettingsResponse(site.getThemeCode(), null, null, null, null, null,
                        null, null));
    }

    private SiteSettings loadOrCreateSettings(Site site) {
        return siteSettingsDao.findBySite(site)
                .orElseGet(() -> {
                    SiteSettings settings = new SiteSettings();
                    settings.setSite(site);
                    return siteSettingsDao.create(settings);
                });
    }

    private SiteSettingsResponse toResponse(SiteSettings settings) {
        return new SiteSettingsResponse(settings.getId(), settings.getSiteName(), settings.getLogoMediaId(),
                settings.getFooterText(), settings.getContactEmail(), settings.getPhone(),
                settings.getFacebookUrl(), settings.getLinkedinUrl());
    }

    private PublicSiteSettingsResponse toPublicResponse(Site site, SiteSettings settings) {
        return new PublicSiteSettingsResponse(site.getThemeCode(), settings.getSiteName(), settings.getLogoMediaId(),
                settings.getFooterText(), settings.getContactEmail(), settings.getPhone(),
                settings.getFacebookUrl(), settings.getLinkedinUrl());
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static Site requireSite(Site site) {
        if (site == null || site.getId() == null) {
            throw new SiteSettingsServiceException(VALIDATION_ERROR, "Site is required.");
        }
        return site;
    }
}
