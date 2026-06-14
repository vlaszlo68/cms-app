package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseProperty;
import hu.laci.cms.backend.model.common.JoinSpec;
import hu.laci.cms.backend.model.common.QuerySpec;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseDaoBooleanMappingTest {

    private BooleanEntityDao dao;

    @BeforeAll
    static void initializeDatabase() throws SQLException {
        DatabaseConfig.initialize(createEmptyServletContext());
        createTestTable();
        createJoinTestTables();
    }

    @AfterAll
    static void shutdownDatabase() throws SQLException {
        dropJoinTestTables();
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
    void customFindOneMapsProjection() throws SQLException {
        insertRawActiveValue("T");
        insertRawActiveValue("F");

        Optional<BooleanSummary> summary = dao.findCustomSummary();

        assertTrue(summary.isPresent());
        assertEquals(2L, summary.orElseThrow().rowCount());
        assertEquals(1L, summary.orElseThrow().activeCount());
    }

    @Test
    void customFindListMapsProjectionRows() throws SQLException {
        long activeId = insertRawActiveValue("T");
        long inactiveId = insertRawActiveValue("F");

        List<BooleanRow> rows = dao.findCustomRows();

        assertEquals(List.of(new BooleanRow(activeId, true), new BooleanRow(inactiveId, false)), rows);
    }

    @Test
    void customUpdateSupportsInsertUpdateAndDelete() throws SQLException {
        assertEquals(1, dao.insertCustomActive(true));
        BooleanRow row = dao.findCustomRows().get(0);
        assertEquals(Boolean.TRUE, row.active());
        assertEquals("T", findRawActiveValue(row.id()));

        assertEquals(1, dao.updateCustomActive(row.id(), false));
        assertEquals("F", findRawActiveValue(row.id()));

        assertEquals(1, dao.deleteAllCustom());
        assertTrue(dao.findCustomRows().isEmpty());
    }

    @Test
    void invalidBooleanDatabaseValueFailsFast() throws SQLException {
        long entityId = insertRawActiveValue("X");

        assertThrows(DataAccessException.class, () -> dao.findById(entityId));
    }

    @Test
    void findAllWithJoinMapsJoinedEntityReference() throws SQLException {
        ChildEntityDao childDao = new ChildEntityDao();
        long grandParentId = insertJoinGrandParent("grand-parent-alpha");
        long parentId = insertJoinParent(grandParentId, "parent-alpha");
        long childId = insertJoinChild(parentId, "child-alpha");

        List<ChildEntity> children = childDao.findAll(QuerySpec.<ChildProperty>create()
                .leftJoin(ParentEntity.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.PARENT,
                        "parent_alias")
                .leftJoin(GrandParentEntity.class, ParentProperty.GRAND_PARENT, GrandParentProperty.ID,
                        ParentProperty.GRAND_PARENT)
                .where(ParentProperty.NAME.withAlias("parent_alias")).equalsTo("parent-alpha"));

        assertEquals(1, children.size());
        assertEquals(childId, children.get(0).getId());
        assertEquals("child-alpha", children.get(0).getName());
        assertEquals(parentId, children.get(0).getParent().getId());
        assertEquals("parent-alpha", children.get(0).getParent().getName());
        assertEquals(grandParentId, children.get(0).getParent().getGrandParent().getId());
        assertEquals("grand-parent-alpha", children.get(0).getParent().getGrandParent().getName());
    }

    @Test
    void findAllIncludesJoinClauseWhenFilteringByJoinedEntity() throws SQLException {
        ChildEntityDao childDao = new ChildEntityDao();
        long grandParentId = insertJoinGrandParent("grand-parent-alpha");
        long matchingParentId = insertJoinParent(grandParentId, "matching-parent");
        long otherParentId = insertJoinParent(grandParentId, "other-parent");
        long matchingChildId = insertJoinChild(matchingParentId, "matching-child");
        insertJoinChild(otherParentId, "other-child");

        List<ChildEntity> children = childDao.findAll(QuerySpec.<ChildProperty>create()
                .leftJoin(ParentEntity.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.PARENT,
                        "filtered_parent")
                .where(ParentProperty.NAME.withAlias("filtered_parent")).equalsTo("matching-parent"));

        assertEquals(1, children.size());
        assertEquals(matchingChildId, children.get(0).getId());
        assertEquals("matching-child", children.get(0).getName());
        assertEquals(matchingParentId, children.get(0).getParent().getId());
        assertEquals("matching-parent", children.get(0).getParent().getName());
    }

    @Test
    void findAllRejectsDuplicateJoinAlias() throws SQLException {
        ChildEntityDao childDao = new ChildEntityDao();

        QuerySpec<ChildProperty> querySpec = QuerySpec.<ChildProperty>create()
                .leftJoin(ParentEntity.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.PARENT,
                        "duplicate_alias")
                .leftJoin(GrandParentEntity.class, ParentProperty.GRAND_PARENT, GrandParentProperty.ID,
                        ParentProperty.GRAND_PARENT, "duplicate_alias");

        assertThrows(IllegalArgumentException.class, () -> childDao.findAll(querySpec));
    }

    @Test
    void findAllSupportsJoiningSameTableTwiceWithDifferentAliases() throws SQLException {
        ChildEntityDao childDao = new ChildEntityDao();
        long grandParentId = insertJoinGrandParent("grand-parent-alpha");
        long primaryParentId = insertJoinParent(grandParentId, "primary-parent");
        long secondaryParentId = insertJoinParent(grandParentId, "secondary-parent");
        long childId = insertJoinChild(primaryParentId, secondaryParentId, "child-alpha");

        List<ChildEntity> children = childDao.findAll(QuerySpec.<ChildProperty>create()
                .leftJoin(ParentEntity.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.PARENT,
                        "primary_parent")
                .leftJoin(ParentEntity.class, ChildProperty.SECOND_PARENT, ParentProperty.ID,
                        ChildProperty.SECOND_PARENT, "secondary_parent")
                .where(ParentProperty.NAME.withAlias("secondary_parent")).equalsTo("secondary-parent"));

        assertEquals(1, children.size());
        assertEquals(childId, children.get(0).getId());
        assertEquals(primaryParentId, children.get(0).getParent().getId());
        assertEquals("primary-parent", children.get(0).getParent().getName());
        assertEquals(secondaryParentId, children.get(0).getSecondParent().getId());
        assertEquals("secondary-parent", children.get(0).getSecondParent().getName());
    }

    @Test
    void findAllRejectsJoiningSameEntityMultipleTimesWithoutExplicitAliases() throws SQLException {
        ChildEntityDao childDao = new ChildEntityDao();

        QuerySpec<ChildProperty> querySpec = QuerySpec.<ChildProperty>create()
                .leftJoin(ParentEntity.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.PARENT)
                .leftJoin(ParentEntity.class, ChildProperty.SECOND_PARENT, ParentProperty.ID,
                        ChildProperty.SECOND_PARENT);

        assertThrows(IllegalArgumentException.class, () -> childDao.findAll(querySpec));
    }

    @Test
    void findAllSupportsExtraJoinConditionInOnClause() throws SQLException {
        ChildEntityDao childDao = new ChildEntityDao();
        long grandParentId = insertJoinGrandParent("grand-parent-alpha");
        long parentId = insertJoinParent(grandParentId, "actual-parent");
        long childId = insertJoinChild(parentId, "child-alpha");

        JoinSpec parentJoin = JoinSpec.left(ParentEntity.class, ChildProperty.PARENT, ParentProperty.ID,
                ChildProperty.PARENT, "filtered_parent")
                .on(ParentProperty.NAME.withAlias("filtered_parent")).equalsTo("different-parent");

        List<ChildEntity> children = childDao.findAll(QuerySpec.<ChildProperty>create()
                .join(parentJoin)
                .where(ChildProperty.ID).equalsTo(childId));

        assertEquals(1, children.size());
        assertEquals(childId, children.get(0).getId());
        assertNull(children.get(0).getParent());
    }

    @Test
    void findAllRejectsNonEntityJoinTargetProperty() throws SQLException {
        ChildEntityDao childDao = new ChildEntityDao();

        QuerySpec<ChildProperty> querySpec = QuerySpec.<ChildProperty>create()
                .leftJoin(ParentEntity.class, ChildProperty.PARENT, ParentProperty.ID, ChildProperty.NAME);

        assertThrows(IllegalArgumentException.class, () -> childDao.findAll(querySpec));
    }

    @Test
    void findAllRejectsNestedJoinBeforeTargetOwnerIsLoaded() throws SQLException {
        ChildEntityDao childDao = new ChildEntityDao();

        QuerySpec<ChildProperty> querySpec = QuerySpec.<ChildProperty>create()
                .leftJoin(GrandParentEntity.class, ParentProperty.GRAND_PARENT, GrandParentProperty.ID,
                        ParentProperty.GRAND_PARENT);

        assertThrows(IllegalArgumentException.class, () -> childDao.findAll(querySpec));
    }

    private static void createTestTable() throws SQLException {
        executeSql("""
                CREATE TABLE IF NOT EXISTS dao_boolean_test_entities (
                    id SERIAL PRIMARY KEY,
                    active VARCHAR(1)
                )
                """);
    }

    private static void createJoinTestTables() throws SQLException {
        executeSql("""
                CREATE TABLE IF NOT EXISTS dao_join_grand_parent_entities (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL
                )
                """);
        executeSql("""
                CREATE TABLE IF NOT EXISTS dao_join_parent_entities (
                    id SERIAL PRIMARY KEY,
                    grand_parent_id INTEGER REFERENCES dao_join_grand_parent_entities(id),
                    name VARCHAR(100) NOT NULL
                )
                """);
        executeSql("""
                CREATE TABLE IF NOT EXISTS dao_join_child_entities (
                    id SERIAL PRIMARY KEY,
                    parent_id INTEGER REFERENCES dao_join_parent_entities(id),
                    second_parent_id INTEGER REFERENCES dao_join_parent_entities(id),
                    name VARCHAR(100) NOT NULL
                )
                """);
    }

    private static void dropTestTable() throws SQLException {
        executeSql("DROP TABLE IF EXISTS dao_boolean_test_entities");
    }

    private static void dropJoinTestTables() throws SQLException {
        executeSql("DROP TABLE IF EXISTS dao_join_child_entities");
        executeSql("DROP TABLE IF EXISTS dao_join_parent_entities");
        executeSql("DROP TABLE IF EXISTS dao_join_grand_parent_entities");
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

    private static long insertJoinGrandParent(String name) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO dao_join_grand_parent_entities (name)
                     VALUES (?)
                     RETURNING id
                     """)) {
            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private static long insertJoinParent(Long grandParentId, String name) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO dao_join_parent_entities (grand_parent_id, name)
                     VALUES (?, ?)
                     RETURNING id
                     """)) {
            statement.setObject(1, grandParentId);
            statement.setString(2, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private static long insertJoinChild(Long parentId, String name) throws SQLException {
        return insertJoinChild(parentId, null, name);
    }

    private static long insertJoinChild(Long parentId, Long secondParentId, String name) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO dao_join_child_entities (parent_id, second_parent_id, name)
                     VALUES (?, ?, ?)
                     RETURNING id
                     """)) {
            statement.setObject(1, parentId);
            statement.setObject(2, secondParentId);
            statement.setString(3, name);

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

        private int insertCustomActive(Boolean active) {
            return executeCustomUpdate("insertBooleanEntity", """
                    INSERT INTO dao_boolean_test_entities (active)
                    VALUES (?)
                    """, List.of(active), "Failed to insert boolean entity.");
        }

        private int updateCustomActive(Long id, Boolean active) {
            return executeCustomUpdate("updateBooleanEntity", """
                    UPDATE dao_boolean_test_entities
                    SET active = ?
                    WHERE id = ?
                    """, List.of(active, id), "Failed to update boolean entity.");
        }

        private int deleteAllCustom() {
            return executeCustomUpdate("deleteAllBooleanEntities",
                    "DELETE FROM dao_boolean_test_entities",
                    List.of(),
                    "Failed to delete boolean entities.");
        }

        private Optional<BooleanSummary> findCustomSummary() {
            return findCustomOne("booleanSummary", """
                    SELECT COUNT(*) AS row_count,
                           COUNT(*) FILTER (WHERE active = ?) AS active_count
                    FROM dao_boolean_test_entities
                    """, List.of(true),
                    resultSet -> new BooleanSummary(resultSet.getLong("row_count"),
                            resultSet.getLong("active_count")),
                    "Failed to summarize boolean entities.");
        }

        private List<BooleanRow> findCustomRows() {
            return findCustomList("booleanRows", """
                    SELECT id, active
                    FROM dao_boolean_test_entities
                    ORDER BY id
                    """, List.of(),
                    resultSet -> new BooleanRow(resultSet.getLong("id"), "T".equals(resultSet.getString("active"))),
                    "Failed to list boolean entity rows.");
        }
    }

    private record BooleanSummary(long rowCount, long activeCount) {
    }

    private record BooleanRow(long id, Boolean active) {
    }

    private static final class ChildEntityDao extends BaseDao<ChildEntity, ChildProperty> {

        private ChildEntityDao() {
            super(ChildEntity.class);
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

    @DbTable("dao_join_grand_parent_entities")
    private static final class GrandParentEntity extends BaseEntity {

        @DbColumn("name")
        private String name;

        private GrandParentEntity() {
        }

        private String getName() {
            return name;
        }
    }

    @DbTable("dao_join_parent_entities")
    private static final class ParentEntity extends BaseEntity {

        @DbColumn("grand_parent_id")
        private GrandParentEntity grandParent;

        @DbColumn("name")
        private String name;

        private ParentEntity() {
        }

        private GrandParentEntity getGrandParent() {
            return grandParent;
        }

        private String getName() {
            return name;
        }
    }

    @DbTable("dao_join_child_entities")
    private static final class ChildEntity extends BaseEntity {

        @DbColumn("parent_id")
        private ParentEntity parent;

        @DbColumn("second_parent_id")
        private ParentEntity secondParent;

        @DbColumn("name")
        private String name;

        private ChildEntity() {
        }

        private ParentEntity getParent() {
            return parent;
        }

        private ParentEntity getSecondParent() {
            return secondParent;
        }

        private String getName() {
            return name;
        }
    }

    private static final class ParentProperty extends BaseProperty {

        private static final ParentProperty ID = new ParentProperty("id");
        private static final ParentProperty GRAND_PARENT = new ParentProperty("grandParent");
        private static final ParentProperty NAME = new ParentProperty("name");

        private ParentProperty(String propertyName) {
            super(ParentEntity.class, propertyName);
        }
    }

    private static final class GrandParentProperty extends BaseProperty {

        private static final GrandParentProperty ID = new GrandParentProperty("id");

        private GrandParentProperty(String propertyName) {
            super(GrandParentEntity.class, propertyName);
        }
    }

    private static final class ChildProperty extends BaseProperty {

        private static final ChildProperty ID = new ChildProperty("id");
        private static final ChildProperty PARENT = new ChildProperty("parent");
        private static final ChildProperty SECOND_PARENT = new ChildProperty("secondParent");
        private static final ChildProperty NAME = new ChildProperty("name");

        private ChildProperty(String propertyName) {
            super(ChildEntity.class, propertyName);
        }
    }
}
