package hu.laci.cms.backend.dao.user;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserFilter;
import hu.laci.cms.backend.model.user.UserSort;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoImplTest {

    private static final String TEST_PREFIX = "dao_test_";

    private UserDao userDao;

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(createEmptyServletContext());
    }

    @AfterAll
    static void shutdownDatabase() {
        DatabaseConfig.shutdown();
    }

    @BeforeEach
    void setUp() throws SQLException {
        userDao = new UserDaoImpl();
        deleteTestUsers();
    }

    @AfterEach
    void tearDown() throws SQLException {
        deleteTestUsers();
    }

    @Test
    void findByIdReturnsUser() throws SQLException {
        long userId = insertUser("alpha", "alpha", "alpha");

        Optional<User> user = userDao.findById(userId);

        assertTrue(user.isPresent());
        assertEquals(userId, user.get().getId());
        assertEquals(TEST_PREFIX + "alpha", user.get().getUserName());
        assertEquals(TEST_PREFIX + "alpha_login", user.get().getLoginName());
        assertEquals(TEST_PREFIX + "alpha@example.com", user.get().getEmailAddress());
        assertEquals(TEST_PREFIX + "alpha_hash", user.get().getPasswordHash());
    }

    @Test
    void findByIdReturnsEmptyWhenUserDoesNotExist() {
        Optional<User> user = userDao.findById(-1L);

        assertFalse(user.isPresent());
    }

    @Test
    void findByLoginNameReturnsUser() throws SQLException {
        long userId = insertUser("alpha", "alpha", "alpha");

        Optional<User> user = userDao.findByLoginName(TEST_PREFIX + "alpha_login");

        assertTrue(user.isPresent());
        assertEquals(userId, user.get().getId());
    }

    @Test
    void findByEmailReturnsUser() throws SQLException {
        long userId = insertUser("alpha", "alpha", "alpha");

        Optional<User> user = userDao.findByEmail(TEST_PREFIX + "alpha@example.com");

        assertTrue(user.isPresent());
        assertEquals(userId, user.get().getId());
    }

    @Test
    void findAllFiltersByIdUsingBaseFilter() throws SQLException {
        long userId = insertUser("alpha", "alpha", "alpha");
        insertUser("beta", "beta", "beta");

        List<User> users = userDao.findAll(new UserFilter(userId, null, null, null), null);

        assertEquals(1, users.size());
        assertEquals(userId, users.get(0).getId());
    }

    @Test
    void findAllFiltersByLikePrefix() throws SQLException {
        insertUser("alpha", "alpha", "alpha");
        insertUser("beta", "beta", "beta");

        List<User> users = userDao.findAll(new UserFilter(TEST_PREFIX + "al", null, null), null);

        assertEquals(1, users.size());
        assertEquals(TEST_PREFIX + "alpha", users.get(0).getUserName());
    }

    @Test
    void findAllUsesDefaultIdAscendingOrder() throws SQLException {
        long firstUserId = insertUser("alpha", "alpha", "alpha");
        long secondUserId = insertUser("beta", "beta", "beta");

        List<User> users = userDao.findAll(new UserFilter(TEST_PREFIX, null, null), null);

        assertEquals(List.of(firstUserId, secondUserId), users.stream().map(User::getId).toList());
    }

    @Test
    void findAllSortsByMultipleFieldsWithDirections() throws SQLException {
        insertUser("same", "charlie", "charlie");
        insertUser("same", "bravo", "bravo");
        insertUser("same", "alpha", "alpha");

        List<User> users = userDao.findAll(
                new UserFilter(TEST_PREFIX + "same", null, null),
                List.of(UserSort.USER_NAME.asc(), UserSort.LOGIN_NAME.desc()));

        assertEquals(List.of(
                        TEST_PREFIX + "charlie_login",
                        TEST_PREFIX + "bravo_login",
                        TEST_PREFIX + "alpha_login"),
                users.stream().map(User::getLoginName).toList());
    }

    private long insertUser(String userNameSuffix, String loginNameSuffix, String emailSuffix) throws SQLException {
        String sql = """
                INSERT INTO users (username, login_name, email_address, password_hash)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TEST_PREFIX + userNameSuffix);
            statement.setString(2, TEST_PREFIX + loginNameSuffix + "_login");
            statement.setString(3, TEST_PREFIX + emailSuffix + "@example.com");
            statement.setString(4, TEST_PREFIX + userNameSuffix + "_hash");

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
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
                UserDaoImplTest.class.getClassLoader(),
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
