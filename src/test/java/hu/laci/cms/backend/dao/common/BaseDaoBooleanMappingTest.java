package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseProperty;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseDaoBooleanMappingTest {

    private BooleanEntityDao dao;

    @BeforeAll
    static void initializeDatabase() throws SQLException {
        DatabaseConfig.initialize(createEmptyServletContext());
        createTestTable();
    }

    @AfterAll
    static void shutdownDatabase() throws SQLException {
        dropTestTable();
        DatabaseConfig.shutdown();
    }

    @BeforeEach
    void setUp() throws SQLException {
        dao = new BooleanEntityDao();
        deleteTestRows();
    }

    @AfterEach
    void tearDown() throws SQLException {
        deleteTestRows();
    }

    @Test
    void createMapsTrueToUppercaseT() throws SQLException {
        BooleanEntity entity = dao.create(new BooleanEntity(null, true));

        assertEquals("T", findRawActiveValue(entity.getId()));
    }

    @Test
    void createMapsFalseToUppercaseF() throws SQLException {
        BooleanEntity entity = dao.create(new BooleanEntity(null, false));

        assertEquals("F", findRawActiveValue(entity.getId()));
    }

    @Test
    void findByIdMapsUppercaseTAndFToBoolean() throws SQLException {
        long activeId = insertRawActiveValue("T");
        long inactiveId = insertRawActiveValue("F");

        assertTrue(dao.findById(activeId).orElseThrow().getActive());
        assertFalse(dao.findById(inactiveId).orElseThrow().getActive());
    }

    @Test
    void updateMapsBooleanToUppercaseValue() throws SQLException {
        BooleanEntity entity = dao.create(new BooleanEntity(null, true));
        entity.setActive(false);

        dao.update(entity);

        assertEquals("F", findRawActiveValue(entity.getId()));
        assertFalse(dao.findById(entity.getId()).orElseThrow().getActive());
    }

    @Test
    void nullableBooleanStaysNull() throws SQLException {
        BooleanEntity entity = dao.create(new BooleanEntity(null, null));

        assertNull(findRawActiveValue(entity.getId()));
        assertNull(dao.findById(entity.getId()).orElseThrow().getActive());
    }

    @Test
    void invalidBooleanDatabaseValueFailsFast() throws SQLException {
        long entityId = insertRawActiveValue("X");

        assertThrows(DataAccessException.class, () -> dao.findById(entityId));
    }

    private static void createTestTable() throws SQLException {
        executeSql("""
                CREATE TABLE IF NOT EXISTS dao_boolean_test_entities (
                    id SERIAL PRIMARY KEY,
                    active VARCHAR(1)
                )
                """);
    }

    private static void dropTestTable() throws SQLException {
        executeSql("DROP TABLE IF EXISTS dao_boolean_test_entities");
    }

    private static void deleteTestRows() throws SQLException {
        executeSql("DELETE FROM dao_boolean_test_entities");
    }

    private static void executeSql(String sql) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private static long insertRawActiveValue(String activeValue) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO dao_boolean_test_entities (active)
                     VALUES (?)
                     RETURNING id
                     """)) {
            statement.setString(1, activeValue);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private static String findRawActiveValue(Long id) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT active
                     FROM dao_boolean_test_entities
                     WHERE id = ?
                     """)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString("active");
            }
        }
    }

    private static ServletContext createEmptyServletContext() {
        return (ServletContext) Proxy.newProxyInstance(
                BaseDaoBooleanMappingTest.class.getClassLoader(),
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

    private static final class BooleanEntityDao extends BaseDao<BooleanEntity, BaseProperty> {

        private BooleanEntityDao() {
            super(BooleanEntity.class);
        }
    }

    @DbTable("dao_boolean_test_entities")
    private static final class BooleanEntity extends BaseEntity {

        @DbColumn("active")
        private Boolean active;

        private BooleanEntity() {
        }

        private BooleanEntity(Long id, Boolean active) {
            setId(id);
            this.active = active;
        }

        private Boolean getActive() {
            return active;
        }

        private void setActive(Boolean active) {
            this.active = active;
        }
    }
}
