package hu.laci.cms.backend.model.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regression coverage for MenuItem query metadata after the SiteApplication refactor. */
class MenuItemPropertyTest {

    @Test
    void siteApplicationPropertyMatchesTheRenamedMenuItemRelation() {
        assertEquals("siteApplication", MenuItemProperty.SITE_APPLICATION.getPropertyName());
    }
}
