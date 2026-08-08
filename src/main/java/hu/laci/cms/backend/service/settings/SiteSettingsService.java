package hu.laci.cms.backend.service.settings;

import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dto.settings.SaveSiteSettingsRequest;
import hu.laci.cms.backend.dto.settings.PublicSiteSettingsResponse;
import hu.laci.cms.backend.dto.settings.SiteSettingsResponse;
import hu.laci.cms.backend.model.settings.SiteSettings;

import java.util.Objects;

/**
 * Reads and updates the single global site settings record.
 */
public class SiteSettingsService {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final SiteSettingsDao siteSettingsDao;

    public SiteSettingsService(SiteSettingsDao siteSettingsDao) {
        this.siteSettingsDao = Objects.requireNonNull(siteSettingsDao, "siteSettingsDao must not be null");
    }

    public SiteSettingsResponse getSettings() {
        return toResponse(loadOrCreateSettings());
    }

    public SiteSettingsResponse saveSettings(SaveSiteSettingsRequest request) {
        if (request == null) {
            throw new SiteSettingsServiceException(VALIDATION_ERROR, "Request body is required.");
        }

        SiteSettings settings = loadOrCreateSettings();
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
    public PublicSiteSettingsResponse getPublicSettings() {
        return siteSettingsDao.findSettings()
                .map(this::toPublicResponse)
                .orElseGet(() -> new PublicSiteSettingsResponse(null, null, null, null, null, null, null));
    }

    private SiteSettings loadOrCreateSettings() {
        return siteSettingsDao.findSettings()
                .orElseGet(() -> siteSettingsDao.create(new SiteSettings()));
    }

    private SiteSettingsResponse toResponse(SiteSettings settings) {
        return new SiteSettingsResponse(settings.getId(), settings.getSiteName(), settings.getLogoMediaId(),
                settings.getFooterText(), settings.getContactEmail(), settings.getPhone(),
                settings.getFacebookUrl(), settings.getLinkedinUrl());
    }

    private PublicSiteSettingsResponse toPublicResponse(SiteSettings settings) {
        return new PublicSiteSettingsResponse(settings.getSiteName(), settings.getLogoMediaId(),
                settings.getFooterText(), settings.getContactEmail(), settings.getPhone(),
                settings.getFacebookUrl(), settings.getLinkedinUrl());
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
