package hu.laci.cms.backend.service.e2e;

import hu.laci.cms.backend.dao.application.SiteApplicationDao;
import hu.laci.cms.backend.dao.site.SiteDao;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

/**
 * Idempotently provisions explicitly opt-in local E2E users and applications.
 *
 * <p>This service is called only when {@code CMS_E2E_PUBLIC_AUTH_ENABLED=true}.
 * It never runs from a database migration and touches only the reserved
 * {@code e2e.*@example.test} user identities and {@code E2E_*} applications.</p>
 */
public final class E2ePublicAuthFixtureService {

    /** Site that hosts the local fixture applications. */
    public static final String SITE_CODE = "default";
    /** Anonymous application code. */
    public static final String PUBLIC_APPLICATION_CODE = "E2E_PUBLIC_APP";
    /** Authenticated application code. */
    public static final String AUTHENTICATED_APPLICATION_CODE = "E2E_AUTH_APP";
    /** Anonymous function code. */
    public static final String PUBLIC_FUNCTION_CODE = "PUBLIC_PING";
    /** Authenticated function code. */
    public static final String AUTHENTICATED_FUNCTION_CODE = "AUTH_ECHO";
    /** Administrator-only function code. */
    public static final String ADMIN_FUNCTION_CODE = "ADMIN_ECHO";
    /** Reserved local USER identity. */
    public static final String USER_EMAIL = "e2e.user@example.test";
    /** Reserved local USER password. */
    public static final String USER_PASSWORD = "E2eUser!2026";
    /** Reserved local ADMIN identity. */
    public static final String ADMIN_EMAIL = "e2e.admin@example.test";
    /** Reserved local ADMIN password. */
    public static final String ADMIN_PASSWORD = "E2eAdmin!2026";

    private final SiteDao siteDao;
    private final UserDao userDao;
    private final SiteApplicationDao applicationDao;

    /**
     * Creates the opt-in fixture provisioner.
     *
     * @param siteDao Site lookup boundary
     * @param userDao User persistence boundary
     * @param applicationDao Application persistence boundary
     */
    public E2ePublicAuthFixtureService(SiteDao siteDao, UserDao userDao, SiteApplicationDao applicationDao) {
        this.siteDao = Objects.requireNonNull(siteDao, "siteDao must not be null");
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
        this.applicationDao = Objects.requireNonNull(applicationDao, "applicationDao must not be null");
    }

    /**
     * Creates or resets the reserved local users and applications.
     *
     * <p>Known fixture passwords are reset on each opted-in startup so browser
     * checks remain repeatable. This is safe only because these identities are
     * reserved exclusively for local E2E use.</p>
     */
    public void ensureFixtures() {
        Site site = siteDao.findByCode(SITE_CODE)
                .orElseThrow(() -> new IllegalStateException("E2E fixture Site 'default' is required."));
        ensureUser("E2E User", "e2e-user", USER_EMAIL, USER_PASSWORD, UserRole.USER);
        ensureUser("E2E Admin", "e2e-admin", ADMIN_EMAIL, ADMIN_PASSWORD, UserRole.ADMIN);
        ensureApplication(site, PUBLIC_APPLICATION_CODE, "E2E Public Application");
        ensureApplication(site, AUTHENTICATED_APPLICATION_CODE, "E2E Authenticated Application");
    }

    private void ensureUser(String userName, String loginName, String email, String password, UserRole role) {
        User user = userDao.findByEmail(email).orElseGet(User::new);
        user.setUserName(userName);
        user.setLoginName(loginName);
        user.setEmailAddress(email);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRole(role);
        user.setActive(Boolean.TRUE);
        user.setRegistrationState(RegistrationState.COMPLETED);
        userDao.save(user);
    }

    private void ensureApplication(Site site, String code, String name) {
        SiteApplication siteApplication = applicationDao.findBySiteAndApplicationCode(site, code)
                .orElseGet(() -> new SiteApplication(null, site, code, name, true));
        siteApplication.setName(name);
        siteApplication.setActive(true);
        applicationDao.save(siteApplication);
    }
}
