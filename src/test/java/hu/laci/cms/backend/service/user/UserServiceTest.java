package hu.laci.cms.backend.service.user;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.migration.DatabaseMigrationRunner;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.user.UserDao;
import hu.laci.cms.backend.dao.user.UserDaoImpl;
import hu.laci.cms.backend.dto.user.CreateUserRequest;
import hu.laci.cms.backend.dto.user.UpdateUserRequest;
import hu.laci.cms.backend.dto.user.UserResponse;
import hu.laci.cms.backend.model.user.RegistrationState;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private static final String TEST_PREFIX = "service_test_";

    private UserDao userDao;
    private UserService userService;

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(createEmptyServletContext());
        DatabaseMigrationRunner.runMigrations();
        DaoRegistry.initialize();
    }

    @AfterAll
    static void shutdownDatabase() {
        DaoRegistry.shutdown();
        DatabaseConfig.shutdown();
    }

    @BeforeEach
    void setUp() throws SQLException {
        SessionContext.clear();
        userDao = new UserDaoImpl();
        userService = new UserService(userDao);
        deleteTestUsers();
    }

    @AfterEach
    void tearDown() throws SQLException {
        SessionContext.clear();
        deleteTestUsers();
    }

    @Test
    void createHashesPasswordAndMapsResponseWithoutPasswordHash() {
        UserResponse response = userService.create(createRequest("alpha", "alpha", "alpha",
                "secret", UserRole.ADMIN, true, RegistrationState.EMAIL_VERIFICATION_REQUIRED));

        assertNotNull(response.getId());
        assertEquals(TEST_PREFIX + "alpha_login", response.getLoginName());
        assertEquals(TEST_PREFIX + "alpha", response.getUserName());
        assertEquals(TEST_PREFIX + "alpha@example.com", response.getEmailAddress());
        assertEquals(UserRole.ADMIN, response.getRole());
        assertTrue(response.getActive());
        assertEquals(RegistrationState.EMAIL_VERIFICATION_REQUIRED, response.getRegistrationStatus());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());

        User persistedUser = userDao.findById(response.getId()).orElseThrow();
        assertNotEquals("secret", persistedUser.getPasswordHash());
        assertTrue(BCrypt.checkpw("secret", persistedUser.getPasswordHash()));
    }

    @Test
    void updateChangesUserAndKeepsPasswordWhenPasswordIsBlank() {
        UserResponse createdUser = userService.create(createRequest("before", "before", "before",
                "original", UserRole.USER, true, RegistrationState.PENDING));
        String originalHash = userDao.findById(createdUser.getId()).orElseThrow().getPasswordHash();

        UserResponse updatedUser = userService.update(createdUser.getId(), updateRequest("after", "after", "after",
                " ", UserRole.ADMIN, false, RegistrationState.COMPLETED));

        assertEquals(createdUser.getId(), updatedUser.getId());
        assertEquals(TEST_PREFIX + "after_login", updatedUser.getLoginName());
        assertEquals(TEST_PREFIX + "after", updatedUser.getUserName());
        assertEquals(TEST_PREFIX + "after@example.com", updatedUser.getEmailAddress());
        assertEquals(UserRole.ADMIN, updatedUser.getRole());
        assertFalse(updatedUser.getActive());
        assertEquals(RegistrationState.COMPLETED, updatedUser.getRegistrationStatus());

        User persistedUser = userDao.findById(createdUser.getId()).orElseThrow();
        assertEquals(originalHash, persistedUser.getPasswordHash());
    }

    @Test
    void updateChangesPasswordWhenPasswordIsPresent() {
        UserResponse createdUser = userService.create(createRequest("password-before", "password-before",
                "password-before", "original", UserRole.USER, true, RegistrationState.PENDING));
        String originalHash = userDao.findById(createdUser.getId()).orElseThrow().getPasswordHash();

        userService.update(createdUser.getId(), updateRequest("password-after", "password-after",
                "password-after", "changed", UserRole.USER, true, RegistrationState.PENDING));

        String changedHash = userDao.findById(createdUser.getId()).orElseThrow().getPasswordHash();
        assertNotEquals(originalHash, changedHash);
        assertTrue(BCrypt.checkpw("changed", changedHash));
    }

    @Test
    void deactivateUsesSoftDelete() {
        UserResponse createdUser = userService.create(createRequest("deactivate", "deactivate",
                "deactivate", "secret", UserRole.USER, true, RegistrationState.PENDING));

        UserResponse deactivatedUser = userService.deactivate(createdUser.getId());

        assertFalse(deactivatedUser.getActive());
        User persistedUser = userDao.findById(createdUser.getId()).orElseThrow();
        assertFalse(persistedUser.getActive());
    }

    @Test
    void createRejectsDuplicateLoginName() {
        userService.create(createRequest("duplicate-login", "duplicate-login", "duplicate-login",
                "secret", UserRole.USER, true, RegistrationState.PENDING));

        UserServiceException exception = assertThrows(UserServiceException.class,
                () -> userService.create(createRequest("other", "duplicate-login", "other",
                        "secret", UserRole.USER, true, RegistrationState.PENDING)));

        assertEquals(UserService.DUPLICATE_LOGIN_NAME, exception.getCode());
    }

    @Test
    void createRejectsDuplicateEmailAddress() {
        userService.create(createRequest("duplicate-email", "duplicate-email", "duplicate-email",
                "secret", UserRole.USER, true, RegistrationState.PENDING));

        UserServiceException exception = assertThrows(UserServiceException.class,
                () -> userService.create(createRequest("other-login", "other", "duplicate-email",
                        "secret", UserRole.USER, true, RegistrationState.PENDING)));

        assertEquals(UserService.DUPLICATE_EMAIL_ADDRESS, exception.getCode());
    }

    @Test
    void createRejectsInvalidEmailAddress() {
        UserServiceException exception = assertThrows(UserServiceException.class,
                () -> userService.create(new CreateUserRequest(TEST_PREFIX + "invalid_login",
                        TEST_PREFIX + "invalid", "invalid-email", "secret", UserRole.USER,
                        true, RegistrationState.PENDING)));

        assertEquals(UserService.VALIDATION_ERROR, exception.getCode());
    }

    private static CreateUserRequest createRequest(String userNameSuffix, String loginNameSuffix, String emailSuffix,
                                                   String password, UserRole role, Boolean active,
                                                   RegistrationState registrationState) {
        return new CreateUserRequest(
                TEST_PREFIX + loginNameSuffix + "_login",
                TEST_PREFIX + userNameSuffix,
                TEST_PREFIX + emailSuffix + "@example.com",
                password,
                role,
                active,
                registrationState
        );
    }

    private static UpdateUserRequest updateRequest(String userNameSuffix, String loginNameSuffix, String emailSuffix,
                                                   String password, UserRole role, Boolean active,
                                                   RegistrationState registrationState) {
        return new UpdateUserRequest(
                TEST_PREFIX + loginNameSuffix + "_login",
                TEST_PREFIX + userNameSuffix,
                TEST_PREFIX + emailSuffix + "@example.com",
                password,
                role,
                active,
                registrationState
        );
    }

    private static void deleteTestUsers() throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM users WHERE login_name LIKE ? OR email_address LIKE ? OR username LIKE ?")) {
            statement.setString(1, TEST_PREFIX + "%");
            statement.setString(2, TEST_PREFIX + "%");
            statement.setString(3, TEST_PREFIX + "%");
            statement.executeUpdate();
        }
    }

    private static ServletContext createEmptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(
                UserServiceTest.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> {
                    if ("getInitParameter".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestServletContext";
                    }
                    return getDefaultValue(method.getReturnType());
                });
    }

    private static Object getDefaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == char.class) {
            return (char) 0;
        }

        return null;
    }
}
