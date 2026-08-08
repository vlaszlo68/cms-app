package hu.laci.cms.backend.service.settings;

import hu.laci.cms.backend.dao.settings.SiteSettingsDao;
import hu.laci.cms.backend.dto.settings.PublicSiteSettingsResponse;
import hu.laci.cms.backend.model.settings.SiteSettings;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests for anonymous site-settings fallback behavior. */
class SiteSettingsServicePublicSettingsTest {

    @Test
    void publicSettingsReturnsAllNullFallbackWithoutCreatingSingleton() {
        SiteSettingsDao siteSettingsDao = Mockito.mock(SiteSettingsDao.class);
        Mockito.when(siteSettingsDao.findSettings()).thenReturn(Optional.empty());

        PublicSiteSettingsResponse response = new SiteSettingsService(siteSettingsDao).getPublicSettings();

        assertNull(response.getSiteName());
        assertNull(response.getLogoMediaId());
        assertNull(response.getFooterText());
        assertNull(response.getContactEmail());
        assertNull(response.getPhone());
        assertNull(response.getFacebookUrl());
        assertNull(response.getLinkedinUrl());
        Mockito.verify(siteSettingsDao).findSettings();
        Mockito.verify(siteSettingsDao, Mockito.never()).create(Mockito.any(SiteSettings.class));
        Mockito.verify(siteSettingsDao, Mockito.never()).update(Mockito.any(SiteSettings.class));
    }

    @Test
    void publicSettingsContainsOnlyLayoutValues() {
        SiteSettingsDao siteSettingsDao = Mockito.mock(SiteSettingsDao.class);
        SiteSettings settings = new SiteSettings();
        settings.setSiteName("CMS");
        settings.setLogoMediaId(8L);
        settings.setFooterText("Footer");
        settings.setContactEmail("contact@example.com");
        settings.setPhone("+36 1 234");
        settings.setFacebookUrl("https://facebook.example");
        settings.setLinkedinUrl("https://linkedin.example");
        Mockito.when(siteSettingsDao.findSettings()).thenReturn(Optional.of(settings));

        PublicSiteSettingsResponse response = new SiteSettingsService(siteSettingsDao).getPublicSettings();

        assertEquals("CMS", response.getSiteName());
        assertEquals(8L, response.getLogoMediaId());
        assertEquals("Footer", response.getFooterText());
        assertEquals("contact@example.com", response.getContactEmail());
        assertEquals("+36 1 234", response.getPhone());
        assertEquals("https://facebook.example", response.getFacebookUrl());
        assertEquals("https://linkedin.example", response.getLinkedinUrl());
    }
}
