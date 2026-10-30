package hu.laci.cms.backend.service.site;

import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dto.site.CreateSiteRequest;
import hu.laci.cms.backend.dto.site.SiteResponse;
import hu.laci.cms.backend.dto.site.UpdateSiteRequest;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Unit tests for Site validation, creation, and public active-Site resolution. */
class SiteServiceTest {

    @Test
    void createPersistsValidatedMachineReadableCode() {
        SiteDao siteDao = Mockito.mock(SiteDao.class);
        Mockito.when(siteDao.findByCode("site-a")).thenReturn(Optional.empty());
        Mockito.when(siteDao.create(Mockito.any(Site.class))).thenAnswer(invocation -> {
            Site site = invocation.getArgument(0, Site.class);
            site.setId(10L);
            return site;
        });

        SiteResponse response = new SiteService(siteDao)
                .createSite(new CreateSiteRequest("site-a", " Site A ", true));

        assertEquals(10L, response.getId());
        assertEquals("site-a", response.getCode());
        assertEquals("Site A", response.getName());
    }

    @Test
    void invalidCodeAndDuplicateCodeAreControlledErrors() {
        SiteDao siteDao = Mockito.mock(SiteDao.class);
        SiteService service = new SiteService(siteDao);
        SiteServiceException invalid = assertThrows(SiteServiceException.class,
                () -> service.createSite(new CreateSiteRequest("Site A", "Site A", true)));
        assertEquals(SiteService.VALIDATION_ERROR, invalid.getCode());

        Mockito.when(siteDao.findByCode("site-a"))
                .thenReturn(Optional.of(new Site(1L, "site-a", "Existing", true)));
        SiteServiceException duplicate = assertThrows(SiteServiceException.class,
                () -> service.createSite(new CreateSiteRequest("site-a", "Site A", true)));
        assertEquals(SiteService.DUPLICATE_SITE_CODE, duplicate.getCode());
    }

    @Test
    void inactiveSiteIsHiddenFromPublicResolutionButAvailableToAdminResolution() {
        SiteDao siteDao = Mockito.mock(SiteDao.class);
        Site inactive = new Site(2L, "inactive", "Inactive", false);
        Mockito.when(siteDao.findByCode("inactive")).thenReturn(Optional.of(inactive));
        SiteService service = new SiteService(siteDao);

        assertFalse(service.requireSite("inactive").isActive());
        SiteServiceException exception = assertThrows(SiteServiceException.class,
                () -> service.requireActiveSite("inactive"));
        assertEquals(SiteService.SITE_NOT_FOUND, exception.getCode());
    }

    @Test
    void createDefaultsThemeAndUpdateRejectsUnsupportedTheme() {
        SiteDao siteDao = Mockito.mock(SiteDao.class);
        Mockito.when(siteDao.findByCode("site-a")).thenReturn(Optional.empty());
        Mockito.when(siteDao.create(Mockito.any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SiteService service = new SiteService(siteDao);

        SiteResponse created = service.createSite(new CreateSiteRequest("site-a", "Site A", true));

        assertEquals(ThemeDefinitionRegistry.STANDARD, created.getThemeCode());
        SiteServiceException exception = assertThrows(SiteServiceException.class,
                () -> service.updateSite("site-a", new UpdateSiteRequest("site-a", "Site A", true, "CUSTOM")));
        assertEquals(SiteService.VALIDATION_ERROR, exception.getCode());
    }

    @Test
    void acceptsEveryTrustedThemeCode() {
        List<String> supportedCodes = ThemeDefinitionRegistry.definitions().stream()
                .map(ThemeDefinition::code).toList();
        assertEquals(List.of("STANDARD", "MODERN", "DARK", "CORPORATE", "MINIMAL"), supportedCodes);

        for (String themeCode : supportedCodes) {
            SiteDao siteDao = Mockito.mock(SiteDao.class);
            Mockito.when(siteDao.findByCode("site-a")).thenReturn(Optional.empty());
            Mockito.when(siteDao.create(Mockito.any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));
            SiteResponse response = new SiteService(siteDao)
                    .createSite(new CreateSiteRequest("site-a", "Site A", true, themeCode));
            assertEquals(themeCode, response.getThemeCode());
        }
    }
}
