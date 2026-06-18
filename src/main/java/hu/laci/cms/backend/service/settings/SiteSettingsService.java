package hu.laci.cms.backend.service.settings;

import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dto.settings.SaveSiteSettingsRequest;
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

    private SiteSettings loadOrCreateSettings() {
        return siteSettingsDao.findSettings()
                .orElseGet(() -> siteSettingsDao.create(new SiteSettings()));
    }

    private SiteSettingsResponse toResponse(SiteSettings settings) {
        return new SiteSettingsResponse(settings.getId(), settings.getSiteName(), settings.getLogoMediaId(),
                settings.getFooterText(), settings.getContactEmail(), settings.getPhone(),
                settings.getFacebookUrl(), settings.getLinkedinUrl());
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
