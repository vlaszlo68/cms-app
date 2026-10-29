package hu.laci.cms.backend.service.site;

import hu.laci.cms.backend.dao.application.SiteApplicationDao;
import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dto.site.SiteUsageResponse;
import hu.laci.cms.backend.model.site.Site;

import java.util.Objects;

/**
 * Calculates direct Site-owned business-data usage and safely orchestrates Site deletion.
 *
 * <p>All operations participate in the request transaction established by {@code TransactionFilter}.
 * SiteSettings is lifecycle-dependent configuration and is removed only after usage confirms that no
 * blocking business data exists.</p>
 */
public class SiteUsageService {

    public static final String SITE_IN_USE = "SITE_IN_USE";

    private final SiteDao siteDao;
    private final PageDao pageDao;
    private final MenuDao menuDao;
    private final MediaDao mediaDao;
    private final SiteApplicationDao siteApplicationDao;
    private final SiteSettingsDao siteSettingsDao;

    /** Creates the Site usage and deletion service. */
    public SiteUsageService(SiteDao siteDao, PageDao pageDao, MenuDao menuDao, MediaDao mediaDao,
                            SiteApplicationDao siteApplicationDao, SiteSettingsDao siteSettingsDao) {
        this.siteDao = Objects.requireNonNull(siteDao, "siteDao must not be null");
        this.pageDao = Objects.requireNonNull(pageDao, "pageDao must not be null");
        this.menuDao = Objects.requireNonNull(menuDao, "menuDao must not be null");
        this.mediaDao = Objects.requireNonNull(mediaDao, "mediaDao must not be null");
        this.siteApplicationDao = Objects.requireNonNull(siteApplicationDao, "siteApplicationDao must not be null");
        this.siteSettingsDao = Objects.requireNonNull(siteSettingsDao, "siteSettingsDao must not be null");
    }

    /**
     * Returns all direct business-resource counts that can block Site deletion.
     *
     * @param site persisted Site
     * @return typed deletion usage information
     */
    public SiteUsageResponse getUsage(Site site) {
        requirePersistedSite(site);
        long pageCount = pageDao.countBySite(site);
        long menuCount = menuDao.countBySite(site);
        long mediaCount = mediaDao.countBySite(site);
        boolean deletable = pageCount == 0 && menuCount == 0 && mediaCount == 0;
        return new SiteUsageResponse(pageCount, menuCount, mediaCount, deletable);
    }

    /**
     * Deletes an empty Site and its lifecycle-dependent configuration records.
     *
     * @param site persisted Site
     * @throws SiteServiceException when business data blocks deletion
     */
    public void deleteSite(Site site) {
        requirePersistedSite(site);
        SiteUsageResponse usage = getUsage(site);
        if (!usage.deletable()) {
            throw new SiteServiceException(SITE_IN_USE, "Site has owned business data and cannot be deleted.", usage);
        }
        siteSettingsDao.findBySite(site).ifPresent(siteSettingsDao::delete);
        siteApplicationDao.deleteBySite(site);
        if (!siteDao.delete(site)) {
            throw new SiteServiceException(SiteService.SITE_NOT_FOUND, "Site not found.");
        }
    }

    private static void requirePersistedSite(Site site) {
        if (site == null || site.getId() == null) {
            throw new SiteServiceException(SiteService.VALIDATION_ERROR, "Site is required.");
        }
    }
}
