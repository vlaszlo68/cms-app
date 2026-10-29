package hu.laci.cms.backend.service.site;

import hu.laci.cms.backend.dao.application.ApplicationDao;
import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dto.site.SiteUsageResponse;
import hu.laci.cms.backend.model.settings.SiteSettings;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for Site usage aggregation and dependent SiteSettings deletion. */
class SiteUsageServiceTest {

    private static final Site SITE = new Site(10L, "site-a", "Site A", true);

    @Test
    void reportsAllBlockingCountsAndDoesNotDeleteAnything() {
        Fixtures fixtures = new Fixtures();
        Mockito.when(fixtures.pageDao.countBySite(SITE)).thenReturn(2L);
        Mockito.when(fixtures.menuDao.countBySite(SITE)).thenReturn(3L);
        Mockito.when(fixtures.mediaDao.countBySite(SITE)).thenReturn(4L);
        Mockito.when(fixtures.applicationDao.countBySite(SITE)).thenReturn(5L);
        SiteUsageService service = fixtures.service();

        SiteUsageResponse usage = service.getUsage(SITE);
        SiteServiceException exception = assertThrows(SiteServiceException.class, () -> service.deleteSite(SITE));

        assertEquals(2L, usage.pageCount());
        assertEquals(3L, usage.menuCount());
        assertEquals(4L, usage.mediaCount());
        assertEquals(5L, usage.applicationCount());
        assertFalse(usage.deletable());
        assertEquals(SiteUsageService.SITE_IN_USE, exception.getCode());
        assertEquals(usage, exception.getDetails());
        Mockito.verify(fixtures.settingsDao, Mockito.never()).delete(Mockito.any());
        Mockito.verify(fixtures.siteDao, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void deletesSettingsBeforeEmptySite() {
        Fixtures fixtures = new Fixtures();
        SiteSettings settings = new SiteSettings();
        settings.setId(11L);
        settings.setSite(SITE);
        Mockito.when(fixtures.settingsDao.findBySite(SITE)).thenReturn(Optional.of(settings));
        Mockito.when(fixtures.siteDao.delete(SITE)).thenReturn(true);
        SiteUsageService service = fixtures.service();

        SiteUsageResponse usage = service.getUsage(SITE);
        service.deleteSite(SITE);

        assertTrue(usage.deletable());
        org.mockito.InOrder order = Mockito.inOrder(fixtures.settingsDao, fixtures.siteDao);
        order.verify(fixtures.settingsDao).delete(settings);
        order.verify(fixtures.siteDao).delete(SITE);
    }

    private static final class Fixtures {
        private final SiteDao siteDao = Mockito.mock(SiteDao.class);
        private final PageDao pageDao = Mockito.mock(PageDao.class);
        private final MenuDao menuDao = Mockito.mock(MenuDao.class);
        private final MediaDao mediaDao = Mockito.mock(MediaDao.class);
        private final ApplicationDao applicationDao = Mockito.mock(ApplicationDao.class);
        private final SiteSettingsDao settingsDao = Mockito.mock(SiteSettingsDao.class);

        private SiteUsageService service() {
            return new SiteUsageService(siteDao, pageDao, menuDao, mediaDao, applicationDao, settingsDao);
        }
    }
}
