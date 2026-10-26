package hu.laci.cms.backend.service.site;

import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dao.menu.MenuDao;
import hu.laci.cms.backend.dao.menu.MenuItemDao;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.dto.menu.CreateMenuItemRequest;
import hu.laci.cms.backend.dto.settings.SaveSiteSettingsRequest;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.service.menu.MenuItemService;
import hu.laci.cms.backend.service.menu.MenuServiceException;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.page.PageServiceException;
import hu.laci.cms.backend.service.settings.SiteSettingsService;
import hu.laci.cms.backend.service.settings.SiteSettingsServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Focused service tests for tenant isolation and cross-Site reference rejection. */
class MultiSiteIsolationServiceTest {

    private static final Site SITE_A = new Site(1L, "site-a", "Site A", true);

    @Test
    void pageByIdLookupCannotFallBackToAnotherSite() {
        PageDao pageDao = Mockito.mock(PageDao.class);
        Mockito.when(pageDao.findBySiteAndId(SITE_A, 99L)).thenReturn(Optional.empty());
        PageService service = new PageService(pageDao, Mockito.mock(TemplateDao.class),
                Mockito.mock(PageBlockDao.class));

        PageServiceException exception = assertThrows(PageServiceException.class,
                () -> service.getPage(SITE_A, 99L));

        assertEquals(PageService.PAGE_NOT_FOUND, exception.getCode());
        Mockito.verify(pageDao).findBySiteAndId(SITE_A, 99L);
        Mockito.verify(pageDao, Mockito.never()).findById(Mockito.anyLong());
    }

    @Test
    void menuItemRejectsPageOutsideMenuSite() {
        MenuDao menuDao = Mockito.mock(MenuDao.class);
        MenuItemDao itemDao = Mockito.mock(MenuItemDao.class);
        PageDao pageDao = Mockito.mock(PageDao.class);
        Menu menu = new Menu(10L, "Main", "MAIN", true);
        menu.setSite(SITE_A);
        Mockito.when(menuDao.findBySiteAndId(SITE_A, 10L)).thenReturn(Optional.of(menu));
        Mockito.when(pageDao.findBySiteAndId(SITE_A, 200L)).thenReturn(Optional.empty());
        MenuItemService service = new MenuItemService(menuDao, itemDao, pageDao);

        MenuServiceException exception = assertThrows(MenuServiceException.class,
                () -> service.createItem(SITE_A, 10L,
                        new CreateMenuItemRequest(10L, null, 200L, "Foreign", 0, true)));

        assertEquals(MenuItemService.INVALID_PAGE_TARGET, exception.getCode());
        Mockito.verify(itemDao, Mockito.never()).create(Mockito.any());
    }

    @Test
    void settingsRejectLogoMediaOutsideSettingsSite() {
        SiteSettingsDao settingsDao = Mockito.mock(SiteSettingsDao.class);
        MediaDao mediaDao = Mockito.mock(MediaDao.class);
        Mockito.when(mediaDao.findBySiteAndId(SITE_A, 300L)).thenReturn(Optional.empty());
        SiteSettingsService service = new SiteSettingsService(settingsDao, mediaDao);

        SiteSettingsServiceException exception = assertThrows(SiteSettingsServiceException.class,
                () -> service.saveSettings(SITE_A, new SaveSiteSettingsRequest(
                        "Site A", 300L, null, null, null, null, null)));

        assertEquals(SiteSettingsService.INVALID_LOGO_MEDIA, exception.getCode());
        Mockito.verify(settingsDao, Mockito.never()).create(Mockito.any());
        Mockito.verify(settingsDao, Mockito.never()).update(Mockito.any());
    }
}
