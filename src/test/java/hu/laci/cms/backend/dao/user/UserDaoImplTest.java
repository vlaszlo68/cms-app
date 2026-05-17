package hu.laci.cms.backend.dao.user;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.config.database.TransactionContext;
import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.dao.common.DataAccessException;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.model.common.LikeFilterPosition;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.model.user.UserProperty;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoImplTest {

    private static final String TEST_PREFIX = "dao_test_";

    private UserDao userDao;

    @BeforeAll
    static void initializeDatabase() {
        DatabaseConfig.initialize(createEmptyServletContext());
        DaoRegistry.initialize();
    }

    @AfterAll
    static void shutdownDatabase() {
        DaoRegistry.shutdown();
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
    void findAllWithQuerySpecFiltersById() throws SQLException {
        long userId = insertUser("alpha", "alpha", "alpha");
        insertUser("beta", "beta", "beta");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.ID).equalsTo(userId));

        assertEquals(1, users.size());
        assertEquals(userId, users.get(0).getId());
    }

    @Test
    void findAllWithQuerySpecFiltersByLikePrefix() throws SQLException {
        insertUser("alpha", "alpha", "alpha");
        insertUser("beta", "beta", "beta");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.USER_NAME).like(TEST_PREFIX + "al"));

        assertEquals(1, users.size());
        assertEquals(TEST_PREFIX + "alpha", users.get(0).getUserName());
    }

    @Test
    void findAllWithQuerySpecFiltersByLikeContains() throws SQLException {
        insertUser("alpha", "alpha", "alpha");
        insertUser("beta", "beta", "beta");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.USER_NAME).like("lph", LikeFilterPosition.CONTAINS));

        assertEquals(1, users.size());
        assertEquals(TEST_PREFIX + "alpha", users.get(0).getUserName());
    }

    @Test
    void findAllWithQuerySpecFiltersByRelationalOperation() throws SQLException {
        long firstUserId = insertUser("alpha", "alpha", "alpha");
        long secondUserId = insertUser("beta", "beta", "beta");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.ID).greaterThan(firstUserId));

        assertEquals(List.of(secondUserId), users.stream().map(User::getId).toList());
    }

    @Test
    void findAllWithQuerySpecFiltersByIn() throws SQLException {
        long firstUserId = insertUser("alpha", "alpha", "alpha");
        insertUser("beta", "beta", "beta");
        long thirdUserId = insertUser("gamma", "gamma", "gamma");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.ID).in(firstUserId, thirdUserId));

        assertEquals(List.of(firstUserId, thirdUserId), users.stream().map(User::getId).toList());
    }

    @Test
    void findAllWithQuerySpecFiltersByNotIn() throws SQLException {
        long firstUserId = insertUser("alpha", "alpha", "alpha");
        long secondUserId = insertUser("beta", "beta", "beta");
        long thirdUserId = insertUser("gamma", "gamma", "gamma");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.USER_NAME).like(TEST_PREFIX)
                .where(UserProperty.ID).notIn(secondUserId));

        assertEquals(List.of(firstUserId, thirdUserId), users.stream().map(User::getId).toList());
    }

    @Test
    void findAllWithQuerySpecFiltersByBetween() throws SQLException {
        long firstUserId = insertUser("alpha", "alpha", "alpha");
        long secondUserId = insertUser("beta", "beta", "beta");
        insertUser("gamma", "gamma", "gamma");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.ID).between(firstUserId, secondUserId));

        assertEquals(List.of(firstUserId, secondUserId), users.stream().map(User::getId).toList());
    }

    @Test
    void findAllWithQuerySpecSortsByMultipleFields() throws SQLException {
        insertUser("same", "charlie", "charlie");
        insertUser("same", "bravo", "bravo");
        insertUser("same", "alpha", "alpha");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.USER_NAME).equalsTo(TEST_PREFIX + "same")
                .orderBy(UserProperty.LOGIN_NAME.desc()));

        assertEquals(List.of(
                        TEST_PREFIX + "charlie_login",
                        TEST_PREFIX + "bravo_login",
                        TEST_PREFIX + "alpha_login"),
                users.stream().map(User::getLoginName).toList());
    }

    @Test
    void findAllUsesDefaultIdAscendingOrder() throws SQLException {
        long firstUserId = insertUser("alpha", "alpha", "alpha");
        long secondUserId = insertUser("beta", "beta", "beta");

        List<User> users = userDao.findAll(QuerySpec.<UserProperty>create()
                .where(UserProperty.USER_NAME).like(TEST_PREFIX));

        assertEquals(List.of(firstUserId, secondUserId), users.stream().map(User::getId).toList());
    }

    @Test
    void findAllSortsByMultipleFieldsWithDirections() throws SQLException {
        insertUser("same", "charlie", "charlie");
        insertUser("same", "bravo", "bravo");
        insertUser("same", "alpha", "alpha");

        List<User> users = userDao.findAll(
                QuerySpec.<UserProperty>create()
                        .where(UserProperty.USER_NAME).like(TEST_PREFIX + "same")
                        .orderBy(UserProperty.USER_NAME.asc(), UserProperty.LOGIN_NAME.desc()));

        assertEquals(List.of(
                        TEST_PREFIX + "charlie_login",
                        TEST_PREFIX + "bravo_login",
                        TEST_PREFIX + "alpha_login"),
                users.stream().map(User::getLoginName).toList());
    }

    @Test
    void createInsertsUserAndSetsGeneratedId() {
        User user = new User(null, TEST_PREFIX + "created", TEST_PREFIX + "created_login",
                TEST_PREFIX + "created@example.com", TEST_PREFIX + "created_hash");

        User createdUser = userDao.create(user);

        assertNotNull(createdUser.getId());

        Optional<User> loadedUser = userDao.findById(createdUser.getId());
        assertTrue(loadedUser.isPresent());
        assertEquals(TEST_PREFIX + "created", loadedUser.get().getUserName());
        assertEquals(TEST_PREFIX + "created_login", loadedUser.get().getLoginName());
        assertEquals(TEST_PREFIX + "created@example.com", loadedUser.get().getEmailAddress());
        assertEquals(TEST_PREFIX + "created_hash", loadedUser.get().getPasswordHash());
    }

    @Test
    void updateModifiesExistingUser() {
        User user = userDao.create(new User(null, TEST_PREFIX + "before", TEST_PREFIX + "before_login",
                TEST_PREFIX + "before@example.com", TEST_PREFIX + "before_hash"));
        user.setUserName(TEST_PREFIX + "after");
        user.setLoginName(TEST_PREFIX + "after_login");
        user.setEmailAddress(TEST_PREFIX + "after@example.com");
        user.setPasswordHash(TEST_PREFIX + "after_hash");

        User updatedUser = userDao.update(user);

        assertEquals(user.getId(), updatedUser.getId());

        Optional<User> loadedUser = userDao.findById(user.getId());
        assertTrue(loadedUser.isPresent());
        assertEquals(TEST_PREFIX + "after", loadedUser.get().getUserName());
        assertEquals(TEST_PREFIX + "after_login", loadedUser.get().getLoginName());
        assertEquals(TEST_PREFIX + "after@example.com", loadedUser.get().getEmailAddress());
        assertEquals(TEST_PREFIX + "after_hash", loadedUser.get().getPasswordHash());
    }

    @Test
    void updateRejectsEntityWithoutId() {
        User user = new User(null, TEST_PREFIX + "missing-id", TEST_PREFIX + "missing_id_login",
                TEST_PREFIX + "missing-id@example.com", TEST_PREFIX + "missing_id_hash");

        assertThrows(IllegalArgumentException.class, () -> userDao.update(user));
    }

    @Test
    void updateRejectsEntityWhenIdDoesNotExist() {
        User user = new User(-1L, TEST_PREFIX + "missing-row", TEST_PREFIX + "missing_row_login",
                TEST_PREFIX + "missing-row@example.com", TEST_PREFIX + "missing_row_hash");

        assertThrows(DataAccessException.class, () -> userDao.update(user));
    }

    @Test
    void saveCreatesUserWhenIdIsNull() {
        User user = new User(null, TEST_PREFIX + "save-created", TEST_PREFIX + "save_created_login",
                TEST_PREFIX + "save-created@example.com", TEST_PREFIX + "save_created_hash");

        User savedUser = userDao.save(user);

        assertNotNull(savedUser.getId());
        assertTrue(userDao.findById(savedUser.getId()).isPresent());
    }

    @Test
    void saveUpdatesUserWhenIdIsPresent() {
        User user = userDao.create(new User(null, TEST_PREFIX + "save-before", TEST_PREFIX + "save_before_login",
                TEST_PREFIX + "save-before@example.com", TEST_PREFIX + "save_before_hash"));
        user.setUserName(TEST_PREFIX + "save-after");

        User savedUser = userDao.save(user);

        Optional<User> loadedUser = userDao.findById(savedUser.getId());
        assertTrue(loadedUser.isPresent());
        assertEquals(TEST_PREFIX + "save-after", loadedUser.get().getUserName());
    }

    @Test
    void staticSaveEntityDelegatesToRegisteredDao() {
        User user = new User(null, TEST_PREFIX + "static-save", TEST_PREFIX + "static_save_login",
                TEST_PREFIX + "static-save@example.com", TEST_PREFIX + "static_save_hash");

        User savedUser = BaseDao.saveEntity(user);

        assertNotNull(savedUser.getId());
        assertTrue(userDao.findById(savedUser.getId()).isPresent());
    }

    @Test
    void staticLoadEntityDelegatesToRegisteredDaoAndOverwritesProperties() throws SQLException {
        long userId = insertUser("load-source", "load-source", "load-source");
        User user = new User(userId, "stale", "stale", "stale", "stale");

        User loadedUser = BaseDao.loadEntity(user);

        assertEquals(user, loadedUser);
        assertEquals(userId, user.getId());
        assertEquals(TEST_PREFIX + "load-source", user.getUserName());
        assertEquals(TEST_PREFIX + "load-source_login", user.getLoginName());
        assertEquals(TEST_PREFIX + "load-source@example.com", user.getEmailAddress());
        assertEquals(TEST_PREFIX + "load-source_hash", user.getPasswordHash());
    }

    @Test
    void staticLoadEntityRejectsEntityWithoutId() {
        User user = new User(null, "stale", "stale", "stale", "stale");

        assertThrows(IllegalArgumentException.class, () -> BaseDao.loadEntity(user));
    }

    @Test
    void staticLoadEntityRejectsMissingEntity() {
        User user = new User(-1L, "stale", "stale", "stale", "stale");

        assertThrows(IllegalArgumentException.class, () -> BaseDao.loadEntity(user));
    }

    @Test
    void deleteByIdDeletesExistingUser() {
        User user = userDao.create(new User(null, TEST_PREFIX + "delete", TEST_PREFIX + "delete_login",
                TEST_PREFIX + "delete@example.com", TEST_PREFIX + "delete_hash"));

        boolean deleted = userDao.deleteById(user.getId());

        assertTrue(deleted);
        assertFalse(userDao.findById(user.getId()).isPresent());
    }

    @Test
    void deleteByIdReturnsFalseWhenUserDoesNotExist() {
        boolean deleted = userDao.deleteById(-1L);

        assertFalse(deleted);
    }

    @Test
    void transactionCommitPersistsDaoChanges() throws SQLException {
        Long userId = null;
        try {
            TransactionContext.begin();
            User user = userDao.create(new User(null, TEST_PREFIX + "tx-commit", TEST_PREFIX + "tx_commit_login",
                    TEST_PREFIX + "tx-commit@example.com", TEST_PREFIX + "tx_commit_hash"));
            userId = user.getId();
            TransactionContext.commit();
        } finally {
            TransactionContext.close();
        }

        assertTrue(userDao.findById(userId).isPresent());
    }

    @Test
    void transactionRollbackDiscardsDaoChanges() throws SQLException {
        Long userId = null;
        try {
            TransactionContext.begin();
            User user = userDao.create(new User(null, TEST_PREFIX + "tx-rollback",
                    TEST_PREFIX + "tx_rollback_login", TEST_PREFIX + "tx-rollback@example.com",
                    TEST_PREFIX + "tx_rollback_hash"));
            userId = user.getId();
            TransactionContext.rollback();
        } finally {
            TransactionContext.close();
        }

        assertFalse(userDao.findById(userId).isPresent());
    }

    @Test
    void transactionRollbackOnlyDiscardsDaoChangesOnCommit() throws SQLException {
        Long userId = null;
        try {
            TransactionContext.begin();
            User user = userDao.create(new User(null, TEST_PREFIX + "tx-rollback-only",
                    TEST_PREFIX + "tx_rollback_only_login", TEST_PREFIX + "tx-rollback-only@example.com",
                    TEST_PREFIX + "tx_rollback_only_hash"));
            userId = user.getId();
            TransactionContext.setRollbackOnly();
            TransactionContext.commit();
        } finally {
            TransactionContext.close();
        }

        assertFalse(userDao.findById(userId).isPresent());
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
