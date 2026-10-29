package hu.laci.cms.backend.service.e2e;

import hu.laci.cms.backend.dao.application.SiteApplicationDao;
import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class E2ePublicAuthFixtureServiceTest {

    @Test
    void provisionsOnlyReservedUsersAndApplications() {
        SiteDao siteDao = mock(SiteDao.class);
        UserDao userDao = mock(UserDao.class);
        SiteApplicationDao applicationDao = mock(SiteApplicationDao.class);
        Site site = new Site();
        site.setCode(E2ePublicAuthFixtureService.SITE_CODE);
        when(siteDao.findByCode(E2ePublicAuthFixtureService.SITE_CODE)).thenReturn(Optional.of(site));
        when(userDao.findByEmail(any())).thenReturn(Optional.empty());
        when(applicationDao.findBySiteAndApplicationCode(any(), any())).thenReturn(Optional.empty());

        new E2ePublicAuthFixtureService(siteDao, userDao, applicationDao).ensureFixtures();

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userDao, times(2)).save(users.capture());
        assertFixtureUser(users.getAllValues().get(0), E2ePublicAuthFixtureService.USER_EMAIL, UserRole.USER);
        assertFixtureUser(users.getAllValues().get(1), E2ePublicAuthFixtureService.ADMIN_EMAIL, UserRole.ADMIN);
        ArgumentCaptor<SiteApplication> applications = ArgumentCaptor.forClass(SiteApplication.class);
        verify(applicationDao, times(2)).save(applications.capture());
        assertEquals(E2ePublicAuthFixtureService.PUBLIC_APPLICATION_CODE, applications.getAllValues().get(0).getApplicationCode());
        assertEquals(E2ePublicAuthFixtureService.AUTHENTICATED_APPLICATION_CODE, applications.getAllValues().get(1).getApplicationCode());
        assertTrue(applications.getAllValues().stream().allMatch(SiteApplication::isActive));
    }

    private void assertFixtureUser(User user, String email, UserRole role) {
        assertEquals(email, user.getEmailAddress());
        assertEquals(role, user.getRole());
        assertEquals(Boolean.TRUE, user.getActive());
        assertEquals(RegistrationState.COMPLETED, user.getRegistrationState());
        assertTrue(user.getPasswordHash().startsWith("$2"));
    }
}
