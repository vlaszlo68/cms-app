package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.config.database.TransactionContext;
import hu.laci.cms.backend.dao.common.annotations.DbColumn;
import hu.laci.cms.backend.dao.common.annotations.DbTable;
import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.FilterOperation;
import hu.laci.cms.backend.model.common.JoinSpec;
import hu.laci.cms.backend.model.common.JoinType;
import hu.laci.cms.backend.model.common.LikeFilterPosition;
import hu.laci.cms.backend.model.common.BaseProperty;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.common.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class BaseDao<T extends BaseEntity, P extends BaseProperty>
        implements CrudDao<T, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDao.class);

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<? extends BaseEntity>, EntityMetadata> ENTITY_METADATA_CACHE = new ConcurrentHashMap<>();

    private final Class<T> entityClass;
    private final String baseSelectSql;
    private final String findByIdSql;
    private final String insertSql;
    private final String updateSql;
    private final String deleteByIdSql;

    protected BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.baseSelectSql = buildBaseSelectSql(entityClass);
        this.findByIdSql = buildFindBySql(baseSelectSql, entityClass, "id");
        this.insertSql = buildInsertSql(entityClass);
        this.updateSql = buildUpdateSql(entityClass);
        this.deleteByIdSql = "DELETE FROM " + getEntityMetadata(entityClass).tableName() + " WHERE "
                + getRequiredQualifiedColumnName(entityClass, "id") + " = ?";
    }

    @SuppressWarnings("unchecked")
    public static <E extends BaseEntity> E saveEntity(E entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }

        CrudDao<E, ? extends BaseProperty> dao = DaoRegistry.getDao(entity.getClass());
        return dao.save(entity);
    }

    @SuppressWarnings("unchecked")
    public static <E extends BaseEntity> E loadEntity(E entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Entity id must not be null for load.");
        }

        CrudDao<E, ? extends BaseProperty> dao = DaoRegistry.getDao(entity.getClass());
        E loadedEntity = dao.findById(entity.getId())
                .orElseThrow(() -> new IllegalArgumentException("No "
                        + entity.getClass().getSimpleName() + " found by id: " + entity.getId()));
        copyEntityProperties(loadedEntity, entity);
        return entity;
    }

    @Override
    public List<T> findAll(QuerySpec<P> querySpec) {
        return findAll(querySpec, "Failed to get " + entityClass.getSimpleName() + " list.");
    }

    @Override
    public Optional<T> findById(Long id) {
        return findById(id, "Failed to find " + entityClass.getSimpleName() + " by id: " + id);
    }

    @Override
    public T save(T entity) {
        if (entity.getId() == null) {
            return create(entity);
        }

        return update(entity);
    }

    @Override
    public T create(T entity) {
        EntityMetadata entityMetadata = getEntityMetadata(entityClass);
        List<SqlParameter> parameters = entityMetadata.insertableColumnFields().stream()
                .map(columnField -> new SqlParameter(columnField.columnName(),
                        getSqlParameterValue(columnField.field(), entity)))
                .toList();

        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement preparedStatement = connectionScope.getConnection().prepareStatement(insertSql)) {
            logSql("create", insertSql, parameters);
            setParameters(preparedStatement, parameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("No id returned.");
                }

                entity.setId(resultSet.getLong("id"));
                LOGGER.info("Created {} id={}", entityClass.getSimpleName(), entity.getId());
                return entity;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to create {}", entityClass.getSimpleName(), e);
            throw new DataAccessException("Failed to create " + entityClass.getSimpleName() + ".", e);
        }
    }

    @Override
    public T update(T entity) {
        requireEntityId(entity);

        EntityMetadata entityMetadata = getEntityMetadata(entityClass);
        List<SqlParameter> parameters = new ArrayList<>();
        for (ColumnFieldMetadata columnField : entityMetadata.updatableColumnFields()) {
            parameters.add(new SqlParameter(columnField.columnName(), getSqlParameterValue(columnField.field(), entity)));
        }
        parameters.add(new SqlParameter("id", entity.getId()));

        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement preparedStatement = connectionScope.getConnection().prepareStatement(updateSql)) {
            logSql("update", updateSql, parameters);
            setParameters(preparedStatement, parameters);
            int updatedRows = preparedStatement.executeUpdate();
            if (updatedRows == 0) {
                throw new DataAccessException("No " + entityClass.getSimpleName()
                        + " found to update by id: " + entity.getId());
            }

            LOGGER.info("Updated {} id={}, rows={}", entityClass.getSimpleName(), entity.getId(), updatedRows);
            return entity;
        } catch (SQLException e) {
            LOGGER.error("Failed to update {} id={}", entityClass.getSimpleName(), entity.getId(), e);
            throw new DataAccessException("Failed to update " + entityClass.getSimpleName()
                    + " by id: " + entity.getId(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        List<SqlParameter> parameters = List.of(new SqlParameter("id", id));
        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement preparedStatement = connectionScope.getConnection().prepareStatement(deleteByIdSql)) {
            logSql("deleteById", deleteByIdSql, parameters);
            setParameters(preparedStatement, parameters);
            boolean deleted = preparedStatement.executeUpdate() > 0;
            LOGGER.info("Deleted {} id={}, deleted={}", entityClass.getSimpleName(), id, deleted);
            return deleted;
        } catch (SQLException e) {
            LOGGER.error("Failed to delete {} id={}", entityClass.getSimpleName(), id, e);
            throw new DataAccessException("Failed to delete " + entityClass.getSimpleName() + " by id: " + id, e);
        }
    }

    protected Class<T> getEntityClass() {
        return entityClass;
    }

    protected String getBaseSelectSql() {
        return baseSelectSql;
    }

    protected RowMapper<T> getRowMapper() {
        return this::mapEntity;
    }

    protected static String buildBaseSelectSql(Class<? extends BaseEntity> entityClass) {
        return buildSelectSql(entityClass, List.of());
    }

    protected static String buildSelectSql(Class<? extends BaseEntity> entityClass, List<JoinSpec> joins) {
        EntityMetadata entityMetadata = getEntityMetadata(entityClass);
        List<String> selectExpressions = new ArrayList<>(entityMetadata.selectExpressions());
        if (joins != null) {
            for (JoinSpec join : joins) {
                String tableAlias = getJoinSqlAlias(join);
                selectExpressions.addAll(getEntityMetadata(join.getEntityClass()).columnFields().stream()
                        .map(columnField -> columnField.selectExpression(tableAlias))
                        .toList());
            }
        }

        return "SELECT " + String.join(", ", selectExpressions)
                + System.lineSeparator()
                + "FROM " + entityMetadata.tableName()
                + System.lineSeparator();
    }

    protected static String buildInsertSql(Class<? extends BaseEntity> entityClass) {
        EntityMetadata entityMetadata = getEntityMetadata(entityClass);
        List<String> columnNames = entityMetadata.insertableColumnFields().stream()
                .map(ColumnFieldMetadata::columnName)
                .toList();
        String placeholders = columnNames.stream()
                .map(columnName -> "?")
                .collect(Collectors.joining(", "));

        return "INSERT INTO " + entityMetadata.tableName()
                + " (" + String.join(", ", columnNames) + ")"
                + System.lineSeparator()
                + "VALUES (" + placeholders + ")"
                + System.lineSeparator()
                + "RETURNING " + getRequiredColumnName(entityClass, "id");
    }

    protected static String buildUpdateSql(Class<? extends BaseEntity> entityClass) {
        EntityMetadata entityMetadata = getEntityMetadata(entityClass);
        String assignments = entityMetadata.updatableColumnFields().stream()
                .map(columnField -> columnField.columnName() + " = ?")
                .collect(Collectors.joining(", "));

        return "UPDATE " + entityMetadata.tableName()
                + System.lineSeparator()
                + "SET " + assignments
                + System.lineSeparator()
                + "WHERE " + getRequiredQualifiedColumnName(entityClass, "id") + " = ?";
    }

    protected static String buildFindBySql(String baseSelectSql, Class<? extends BaseEntity> entityClass,
                                           String propertyName) {
        return baseSelectSql + buildWhereEqualsCondition(entityClass, propertyName);
    }

    protected static String buildWhereEqualsCondition(Class<? extends BaseEntity> entityClass, String propertyName) {
        return "WHERE " + getRequiredQualifiedColumnName(entityClass, propertyName) + " = ?";
    }

    protected void appendJoins(StringBuilder sqlBuilder, List<Object> parameters,
                               Class<? extends BaseEntity> entityClass, List<JoinSpec> joins) {
        if (joins == null || joins.isEmpty()) {
            return;
        }

        List<JoinSpec> previousJoins = new ArrayList<>();
        for (JoinSpec join : joins) {
            String joinType = switch (join.getType()) {
                case INNER -> "INNER JOIN";
                case LEFT -> "LEFT JOIN";
            };
            String tableName = getEntityMetadata(join.getEntityClass()).tableName();
            String tableAlias = getJoinSqlAlias(join);
            sqlBuilder.append(joinType)
                    .append(" ")
                    .append(tableName);
            if (!tableName.equals(tableAlias)) {
                sqlBuilder.append(" ").append(tableAlias);
            }
            sqlBuilder
                    .append(" ON ")
                    .append(getRequiredQualifiedColumnName(entityClass, join.getLeftProperty(), previousJoins))
                    .append(" = ")
                    .append(getRequiredQualifiedColumnName(join.getEntityClass(), join.getRightProperty(), tableAlias));
            appendJoinConditions(sqlBuilder, parameters, entityClass, join, previousJoins);
            sqlBuilder.append(System.lineSeparator());
            previousJoins.add(join);
        }
    }

    private static void appendJoinConditions(StringBuilder sqlBuilder, List<Object> parameters,
                                             Class<? extends BaseEntity> entityClass, JoinSpec join,
                                             List<JoinSpec> previousJoins) {
        List<JoinSpec> availableJoins = new ArrayList<>(previousJoins);
        availableJoins.add(join);
        for (JoinSpec.JoinCondition condition : join.getConditions()) {
            Object value = condition.getValue();
            if (isEmptyFilterValue(value)) {
                continue;
            }

            String columnName = getRequiredColumnName(resolvePropertyEntityClass(entityClass, condition.getProperty()),
                    condition.getProperty().getPropertyName());
            String qualifiedColumnName = getRequiredQualifiedColumnName(entityClass, condition.getProperty(),
                    availableJoins);
            sqlBuilder.append(" AND ")
                    .append(buildFilterCondition(qualifiedColumnName, condition.getOperation(), value));
            parameters.addAll(buildFilterParameters(columnName, value, condition.getOperation(),
                    condition.getLikePosition()));
        }
    }

    protected void appendOrder(StringBuilder sqlBuilder, Class<? extends BaseEntity> entityClass,
                               List<? extends SortOrder<? extends BaseProperty>> sort, List<JoinSpec> joins) {
        List<? extends SortOrder<? extends BaseProperty>> selectedSort =
                sort == null || sort.isEmpty() ? List.of(new SortOrder<>(BaseProperty.ID)) : sort;

        sqlBuilder.append(System.lineSeparator())
                .append("ORDER BY ")
                .append(selectedSort.stream()
                        .map(sortOrder -> getRequiredQualifiedColumnName(entityClass, sortOrder.getProperty(), joins)
                                + " " + sortOrder.getDirection())
                        .collect(Collectors.joining(", ")));
    }

    protected void appendQueryFilters(StringBuilder sqlBuilder, List<Object> parameters,
                                      Class<? extends BaseEntity> entityClass,
                                      QuerySpec<? extends BaseProperty> querySpec) {
        if (querySpec == null) {
            return;
        }

        List<String> conditions = new ArrayList<>();
        for (QuerySpec.FilterCriterion filter : querySpec.getFilters()) {
            Object value = filter.getValue();
            if (isEmptyFilterValue(value)) {
                continue;
            }

            String columnName = getRequiredColumnName(resolvePropertyEntityClass(entityClass, filter.getProperty()),
                    filter.getProperty().getPropertyName());
            String qualifiedColumnName = getRequiredQualifiedColumnName(entityClass, filter.getProperty(),
                    querySpec.getJoins());
            conditions.add(buildFilterCondition(qualifiedColumnName, filter.getOperation(), value));
            parameters.addAll(buildFilterParameters(columnName, value, filter.getOperation(), filter.getLikePosition()));
        }

        if (conditions.isEmpty()) {
            return;
        }

        sqlBuilder.append(System.lineSeparator()).append("WHERE ");
        sqlBuilder.append(String.join(" AND ", conditions));
    }

    protected Optional<T> findById(Long id, String errorMessage) {
        return findOne(findByIdSql, List.of(new SqlParameter("id", id)), getRowMapper(), errorMessage);
    }

    protected Optional<T> findOneByProperty(String propertyName, Object value, String errorMessage) {
        String columnName = getRequiredColumnName(entityClass, propertyName);
        return findOne(buildFindBySql(baseSelectSql, entityClass, propertyName),
                List.of(new SqlParameter(columnName, value)), getRowMapper(),
                errorMessage);
    }

    protected List<T> findAll(QuerySpec<P> querySpec, String errorMessage) {
        List<JoinSpec> joins = querySpec == null ? List.of() : querySpec.getJoins();
        validateQuerySpec(entityClass, querySpec);
        StringBuilder sqlBuilder = new StringBuilder(joins.isEmpty() ? baseSelectSql : buildSelectSql(entityClass, joins));
        List<Object> parameters = new ArrayList<>();

        appendJoins(sqlBuilder, parameters, entityClass, joins);
        appendQueryFilters(sqlBuilder, parameters, entityClass, querySpec);
        appendOrder(sqlBuilder, entityClass, querySpec == null ? null : querySpec.getSortOrders(), joins);

        return findList(sqlBuilder.toString(), parameters, resultSet -> mapEntity(resultSet, joins), errorMessage);
    }

    private static void validateQuerySpec(Class<? extends BaseEntity> rootEntityClass,
                                          QuerySpec<? extends BaseProperty> querySpec) {
        if (querySpec == null) {
            return;
        }

        validateJoins(rootEntityClass, querySpec.getJoins());
        querySpec.getFilters().forEach(filter -> validateColumnProperty(rootEntityClass, filter.getProperty(),
                querySpec.getJoins()));
        querySpec.getSortOrders().forEach(sortOrder -> validateColumnProperty(rootEntityClass,
                sortOrder.getProperty(), querySpec.getJoins()));
    }

    private static void validateJoins(Class<? extends BaseEntity> rootEntityClass, List<JoinSpec> joins) {
        if (joins == null || joins.isEmpty()) {
            return;
        }

        validateRepeatedJoinAliases(joins);
        Set<String> aliases = new HashSet<>();
        aliases.add(getEntityMetadata(rootEntityClass).tableName());
        Set<Class<? extends BaseEntity>> loadedEntityClasses = new HashSet<>();
        loadedEntityClasses.add(rootEntityClass);

        List<JoinSpec> previousJoins = new ArrayList<>();
        for (JoinSpec join : joins) {
            getEntityMetadata(join.getEntityClass());
            String alias = getJoinSqlAlias(join);
            if (!aliases.add(alias)) {
                throw new IllegalArgumentException("Duplicate SQL table alias in joins: " + alias);
            }

            validateColumnProperty(rootEntityClass, join.getLeftProperty(), previousJoins);
            validateColumnProperty(join.getEntityClass(), join.getRightProperty(), List.of());
            validateTargetProperty(rootEntityClass, join, loadedEntityClasses);
            List<JoinSpec> availableJoins = new ArrayList<>(previousJoins);
            availableJoins.add(join);
            join.getConditions().forEach(condition -> validateColumnProperty(rootEntityClass,
                    condition.getProperty(), availableJoins));

            previousJoins.add(join);
            loadedEntityClasses.add(join.getEntityClass());
        }
    }

    private static void validateRepeatedJoinAliases(List<JoinSpec> joins) {
        Map<Class<? extends BaseEntity>, Long> joinCountsByEntityClass = joins.stream()
                .collect(Collectors.groupingBy(JoinSpec::getEntityClass, Collectors.counting()));

        for (JoinSpec join : joins) {
            if (joinCountsByEntityClass.getOrDefault(join.getEntityClass(), 0L) <= 1) {
                continue;
            }
            if (join.getTableAlias() == null || join.getTableAlias().isBlank()) {
                throw new IllegalArgumentException("Entity joined multiple times requires explicit aliases: "
                        + join.getEntityClass().getName());
            }
        }
    }

    private static void validateColumnProperty(Class<? extends BaseEntity> rootEntityClass, BaseProperty property,
                                               List<JoinSpec> joins) {
        Class<? extends BaseEntity> propertyEntityClass = resolvePropertyEntityClass(rootEntityClass, property);
        getRequiredColumnFieldMetadata(propertyEntityClass, property.getPropertyName());
        validatePropertyAlias(rootEntityClass, property, joins);
        validateUnambiguousProperty(rootEntityClass, property, joins);
    }

    private static void validateTargetProperty(Class<? extends BaseEntity> rootEntityClass, JoinSpec join,
                                               Set<Class<? extends BaseEntity>> loadedEntityClasses) {
        Class<? extends BaseEntity> targetOwnerClass = resolvePropertyEntityClass(rootEntityClass,
                join.getTargetProperty());
        if (!loadedEntityClasses.contains(targetOwnerClass)) {
            throw new IllegalArgumentException("Join target owner must be loaded before target mapping: "
                    + targetOwnerClass.getName() + "." + join.getTargetProperty().getPropertyName());
        }

        Field targetField = getRequiredField(targetOwnerClass, join.getTargetProperty().getPropertyName());
        if (!BaseEntity.class.isAssignableFrom(targetField.getType())) {
            throw new IllegalArgumentException("Join target property must be a BaseEntity reference: "
                    + targetOwnerClass.getName() + "." + targetField.getName());
        }
        if (!targetField.getType().isAssignableFrom(join.getEntityClass())) {
            throw new IllegalArgumentException("Join entity " + join.getEntityClass().getName()
                    + " cannot be assigned to target property " + targetOwnerClass.getName()
                    + "." + targetField.getName());
        }
        getRequiredColumnFieldMetadata(targetOwnerClass, join.getTargetProperty().getPropertyName());
    }

    private static void validatePropertyAlias(Class<? extends BaseEntity> rootEntityClass, BaseProperty property,
                                              List<JoinSpec> joins) {
        if (property.getTableAlias() == null || property.getTableAlias().isBlank()) {
            return;
        }

        String propertyAlias = sanitizeAliasPart(property.getTableAlias());
        if (propertyAlias.equals(getEntityMetadata(rootEntityClass).tableName())) {
            return;
        }

        boolean aliasExists = joins != null && joins.stream()
                .anyMatch(join -> getJoinSqlAlias(join).equals(propertyAlias)
                        && join.getEntityClass() == resolvePropertyEntityClass(rootEntityClass, property));
        if (!aliasExists) {
            throw new IllegalArgumentException("Unknown or mismatched table alias for property "
                    + resolvePropertyEntityClass(rootEntityClass, property).getName()
                    + "." + property.getPropertyName() + ": " + propertyAlias);
        }
    }

    private static void validateUnambiguousProperty(Class<? extends BaseEntity> rootEntityClass, BaseProperty property,
                                                    List<JoinSpec> joins) {
        if (property.getTableAlias() != null && !property.getTableAlias().isBlank()) {
            return;
        }

        Class<? extends BaseEntity> propertyEntityClass = resolvePropertyEntityClass(rootEntityClass, property);
        if (propertyEntityClass == rootEntityClass || joins == null) {
            return;
        }

        long joinCount = joins.stream()
                .filter(join -> join.getEntityClass() == propertyEntityClass)
                .count();
        if (joinCount > 1) {
            throw new IllegalArgumentException("Property requires explicit table alias because entity is joined "
                    + "multiple times: " + propertyEntityClass.getName() + "." + property.getPropertyName());
        }
    }

    protected void setParameters(PreparedStatement preparedStatement, List<?> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            preparedStatement.setObject(index + 1, getParameterValue(parameters.get(index)));
        }
    }

    protected Optional<T> findOne(String sql, List<?> parameters, RowMapper<T> rowMapper, String errorMessage) {
        return findOne("findOne", sql, parameters, rowMapper, errorMessage);
    }

    protected <R> Optional<R> findCustomOne(String operation, String sql, List<?> parameters,
                                            RowMapper<R> rowMapper, String errorMessage) {
        return findOne(operationName(operation, "customFindOne"), sql, parameters, rowMapper, errorMessage);
    }

    protected <R> Optional<R> findOne(String operation, String sql, List<?> parameters,
                                      RowMapper<R> rowMapper, String errorMessage) {
        List<?> selectedParameters = normalizeParameters(parameters);
        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement preparedStatement = connectionScope.getConnection().prepareStatement(sql)) {

            logSql(operationName(operation, "findOne"), sql, selectedParameters);
            setParameters(preparedStatement, selectedParameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(rowMapper.map(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to execute findOne SQL.", e);
            throw new DataAccessException(errorMessage, e);
        }
    }

    protected List<T> findList(String sql, List<?> parameters, RowMapper<T> rowMapper, String errorMessage) {
        return findList("findList", sql, parameters, rowMapper, errorMessage);
    }

    protected <R> List<R> findCustomList(String operation, String sql, List<?> parameters,
                                         RowMapper<R> rowMapper, String errorMessage) {
        return findList(operationName(operation, "customFindList"), sql, parameters, rowMapper, errorMessage);
    }

    protected <R> List<R> findList(String operation, String sql, List<?> parameters,
                                   RowMapper<R> rowMapper, String errorMessage) {
        List<?> selectedParameters = normalizeParameters(parameters);
        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement preparedStatement = connectionScope.getConnection().prepareStatement(sql)) {

            logSql(operationName(operation, "findList"), sql, selectedParameters);
            setParameters(preparedStatement, selectedParameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<R> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(rowMapper.map(resultSet));
                }

                return results;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to execute findList SQL.", e);
            throw new DataAccessException(errorMessage, e);
        }
    }

    protected int executeCustomUpdate(String operation, String sql, List<?> parameters, String errorMessage) {
        List<?> selectedParameters = normalizeParameters(parameters);
        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement preparedStatement = connectionScope.getConnection().prepareStatement(sql)) {
            logSql(operationName(operation, "customUpdate"), sql, selectedParameters);
            setParameters(preparedStatement, selectedParameters);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to execute custom update SQL.", e);
            throw new DataAccessException(errorMessage, e);
        }
    }

    protected T mapEntity(ResultSet resultSet) throws SQLException {
        return mapEntity(resultSet, List.of());
    }

    protected T mapEntity(ResultSet resultSet, List<JoinSpec> joins) throws SQLException {
        T entity = createEntity();
        for (ColumnFieldMetadata columnField : getEntityMetadata(entityClass).columnFields()) {
            setFieldValue(columnField.field(), entity,
                    getColumnValue(resultSet, columnField.resultAlias(), columnField.field()));
        }
        mapJoinedEntities(resultSet, entity, joins);

        return entity;
    }

    private T createEntity() throws SQLException {
        return createEntity(entityClass);
    }

    private static <E extends BaseEntity> E createEntity(Class<E> entityClass) throws SQLException {
        try {
            Constructor<E> constructor = entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Unable to create entity instance: " + entityClass.getName(), e);
        }
    }

    private static void mapJoinedEntities(ResultSet resultSet, BaseEntity entity, List<JoinSpec> joins)
            throws SQLException {
        if (joins == null || joins.isEmpty()) {
            return;
        }

        for (JoinSpec join : joins) {
            BaseEntity joinedEntity = mapJoinedEntity(resultSet, join.getEntityClass(), getJoinSqlAlias(join));
            setJoinedEntity(entity, join.getTargetProperty(), joinedEntity);
        }
    }

    private static void setJoinedEntity(BaseEntity rootEntity, BaseProperty targetProperty, BaseEntity joinedEntity)
            throws SQLException {
        Class<? extends BaseEntity> targetOwnerClass = targetProperty.getEntityClass();
        BaseEntity targetOwner = targetOwnerClass == null || targetOwnerClass == rootEntity.getClass()
                ? rootEntity
                : findMappedEntity(rootEntity, targetOwnerClass);
        if (targetOwner == null) {
            throw new SQLException("Unable to map joined entity. Target owner is not loaded: "
                    + targetOwnerClass.getName());
        }

        Field targetField = getRequiredField(targetOwner.getClass(), targetProperty.getPropertyName());
        if (joinedEntity != null && !targetField.getType().isAssignableFrom(joinedEntity.getClass())) {
            throw new SQLException("Joined entity " + joinedEntity.getClass().getName()
                    + " cannot be assigned to " + targetOwner.getClass().getName()
                    + "." + targetField.getName());
        }

        setFieldValue(targetField, targetOwner, joinedEntity);
    }

    private static BaseEntity findMappedEntity(BaseEntity entity, Class<? extends BaseEntity> targetClass) {
        if (entity == null) {
            return null;
        }
        if (entity.getClass() == targetClass) {
            return entity;
        }

        for (Field field : getAllFields(entity.getClass())) {
            if (!BaseEntity.class.isAssignableFrom(field.getType())) {
                continue;
            }

            Object value = getFieldValue(field, entity);
            BaseEntity foundEntity = findMappedEntity((BaseEntity) value, targetClass);
            if (foundEntity != null) {
                return foundEntity;
            }
        }

        return null;
    }

    private static BaseEntity mapJoinedEntity(ResultSet resultSet, Class<? extends BaseEntity> joinedEntityClass,
                                              String tableAlias) throws SQLException {
        EntityMetadata entityMetadata = getEntityMetadata(joinedEntityClass);
        Object idValue = resultSet.getObject(getRequiredColumnFieldMetadata(joinedEntityClass, "id")
                .resultAlias(tableAlias));
        if (idValue == null) {
            return null;
        }

        BaseEntity joinedEntity = createEntity(joinedEntityClass);
        for (ColumnFieldMetadata columnField : entityMetadata.columnFields()) {
            setFieldValue(columnField.field(), joinedEntity,
                    getColumnValue(resultSet, columnField.resultAlias(tableAlias), columnField.field()));
        }

        return joinedEntity;
    }

    private static void copyEntityProperties(BaseEntity source, BaseEntity target) {
        if (source.getClass() != target.getClass()) {
            throw new IllegalArgumentException("Cannot copy entity properties between different classes: "
                    + source.getClass().getName() + " -> " + target.getClass().getName());
        }

        for (Field field : getAllFields(source.getClass())) {
            try {
                setFieldValue(field, target, getFieldValue(field, source));
            } catch (SQLException e) {
                throw new IllegalArgumentException("Unable to copy entity property "
                        + target.getClass().getName() + "." + field.getName(), e);
            }
        }
    }

    private static void requireEntityId(BaseEntity entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Entity id must not be null for update.");
        }
    }

    private static Object getColumnValue(ResultSet resultSet, String columnName, Field field) throws SQLException {
        if (BaseEntity.class.isAssignableFrom(field.getType())) {
            return getEntityReference(resultSet, columnName, field);
        }
        if (Date.class.isAssignableFrom(field.getType())) {
            return getDateValue(resultSet, columnName, field.getType());
        }
        if (field.getType() == Boolean.class || field.getType() == boolean.class) {
            return getBooleanValue(resultSet, columnName, field.getType());
        }
        if (field.getType() == Long.class || field.getType() == long.class) {
            return getLongValue(resultSet, columnName);
        }

        try {
            return resultSet.getObject(columnName, getResultSetValueType(field.getType()));
        } catch (SQLFeatureNotSupportedException | AbstractMethodError e) {
            return resultSet.getObject(columnName);
        }
    }

    private static Boolean getBooleanValue(ResultSet resultSet, String columnName, Class<?> fieldType)
            throws SQLException {
        String value = resultSet.getString(columnName);
        if (value == null) {
            return fieldType == boolean.class ? Boolean.FALSE : null;
        }

        return switch (value.trim().toUpperCase()) {
            case "T" -> Boolean.TRUE;
            case "F" -> Boolean.FALSE;
            default -> throw new SQLException("Invalid boolean value in column " + columnName + ": " + value);
        };
    }

    private static Long getLongValue(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        if (resultSet.wasNull()) {
            return null;
        }

        return value;
    }

    private static Date getDateValue(ResultSet resultSet, String columnName, Class<?> fieldType) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }

        if (fieldType == java.sql.Date.class) {
            return new java.sql.Date(timestamp.getTime());
        }
        if (fieldType == Timestamp.class) {
            return timestamp;
        }

        return new Date(timestamp.getTime());
    }

    private static BaseEntity getEntityReference(ResultSet resultSet, String columnName, Field field)
            throws SQLException {
        Long id = getLongValue(resultSet, columnName);
        if (id == null) {
            return null;
        }

        try {
            BaseEntity entity = createEntity(field.getType().asSubclass(BaseEntity.class));
            entity.setId(id);
            return entity;
        } catch (SQLException e) {
            throw new SQLException("Unable to create referenced entity instance: " + field.getType().getName(), e);
        }
    }

    private static Class<?> getResultSetValueType(Class<?> fieldType) {
        if (!fieldType.isPrimitive()) {
            return fieldType;
        }

        if (fieldType == int.class) {
            return Integer.class;
        }
        if (fieldType == long.class) {
            return Long.class;
        }
        if (fieldType == boolean.class) {
            return Boolean.class;
        }
        if (fieldType == double.class) {
            return Double.class;
        }
        if (fieldType == float.class) {
            return Float.class;
        }
        if (fieldType == short.class) {
            return Short.class;
        }
        if (fieldType == byte.class) {
            return Byte.class;
        }
        if (fieldType == char.class) {
            return Character.class;
        }

        return fieldType;
    }

    private static String buildFilterCondition(String columnName, FilterOperation operation, Object value) {
        return switch (operation) {
            case EQUALS -> columnName + " = ?";
            case LIKE -> columnName + " LIKE ?";
            case LESS -> columnName + " < ?";
            case LESS_OR_EQUALS -> columnName + " <= ?";
            case GREATER -> columnName + " > ?";
            case GREATER_OR_EQUALS -> columnName + " >= ?";
            case IN -> columnName + " IN (" + buildPlaceholders(getFilterValueCount(value)) + ")";
            case NOT_IN -> columnName + " NOT IN (" + buildPlaceholders(getFilterValueCount(value)) + ")";
            case BETWEEN -> columnName + " BETWEEN ? AND ?";
        };
    }

    private static List<SqlParameter> buildFilterParameters(String columnName, Object value, FilterOperation operation,
                                                            LikeFilterPosition likePosition) {
        return getFilterValues(value, operation).stream()
                .map(filterValue -> new SqlParameter(columnName,
                        buildFilterParameter(filterValue, operation, likePosition)))
                .toList();
    }

    private static Object buildFilterParameter(Object value, FilterOperation operation, LikeFilterPosition likePosition) {
        if (operation == FilterOperation.LIKE && value instanceof String stringValue) {
            String trimmedValue = stringValue.trim();
            return switch (likePosition) {
                case STARTS_WITH -> trimmedValue + "%";
                case ENDS_WITH -> "%" + trimmedValue;
                case CONTAINS -> "%" + trimmedValue + "%";
            };
        }

        return value;
    }

    private static String buildPlaceholders(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> "?")
                .collect(Collectors.joining(", "));
    }

    private static int getFilterValueCount(Object value) {
        return getFilterValues(value, null).size();
    }

    private static List<?> getFilterValues(Object value, FilterOperation operation) {
        if (operation == FilterOperation.BETWEEN) {
            List<?> values = asValueList(value);
            if (values.size() != 2) {
                throw new IllegalArgumentException("BETWEEN filter requires exactly two values.");
            }

            return values;
        }

        if (operation == FilterOperation.IN || operation == FilterOperation.NOT_IN || value instanceof Collection<?>
                || value != null && value.getClass().isArray()) {
            return asValueList(value);
        }

        return List.of(value);
    }

    private static List<?> asValueList(Object value) {
        if (value instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }
        if (value instanceof Object[] array) {
            return Arrays.asList(array);
        }

        return List.of(value);
    }

    private static Object getFieldValue(Field field, Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to read filter property "
                    + instance.getClass().getName() + "." + field.getName(), e);
        }
    }

    private static Field getRequiredField(Class<?> entityClass, String propertyName) {
        return getAllFields(entityClass).stream()
                .filter(field -> field.getName().equals(propertyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing property on "
                        + entityClass.getName() + "." + propertyName));
    }

    private static Object getSqlParameterValue(Field field, Object instance) {
        Object value = getFieldValue(field, instance);
        if (value == null) {
            return null;
        }
        if (value instanceof BaseEntity entity) {
            return entity.getId();
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "T" : "F";
        }
        if (value instanceof Timestamp || value instanceof java.sql.Date) {
            return value;
        }
        if (value instanceof Date dateValue) {
            return new Timestamp(dateValue.getTime());
        }

        return value;
    }

    private static Object getParameterValue(Object parameter) {
        if (parameter instanceof SqlParameter sqlParameter) {
            return getSqlValue(sqlParameter.value());
        }

        return getSqlValue(parameter);
    }

    private static Object getSqlValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "T" : "F";
        }

        return value;
    }

    private static List<?> normalizeParameters(List<?> parameters) {
        return parameters == null ? List.of() : parameters;
    }

    private static String operationName(String operation, String defaultOperation) {
        return operation == null || operation.isBlank() ? defaultOperation : operation;
    }

    private static void logSql(String operation, String sql, List<?> parameters) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        LOGGER.debug("SQL {}: {} | params={}", operation, normalizeSql(sql), formatParameters(parameters));
    }

    private static String normalizeSql(String sql) {
        return sql.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private static List<String> formatParameters(List<?> parameters) {
        return parameters.stream()
                .map(BaseDao::formatParameter)
                .toList();
    }

    private static String formatParameter(Object parameter) {
        if (parameter instanceof SqlParameter sqlParameter) {
            return sqlParameter.name() + "=" + maskValue(sqlParameter.name(), sqlParameter.value());
        }

        return String.valueOf(parameter);
    }

    private static Object maskValue(String name, Object value) {
        if (isSensitiveName(name)) {
            return "***";
        }

        return value;
    }

    private static boolean isSensitiveName(String name) {
        String normalizedName = name == null ? "" : name.toLowerCase();
        return normalizedName.contains("password")
                || normalizedName.contains("hash")
                || normalizedName.contains("token")
                || normalizedName.contains("secret");
    }

    private static void setFieldValue(Field field, Object instance, Object value) throws SQLException {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new SQLException("Unable to set entity property "
                    + instance.getClass().getName() + "." + field.getName(), e);
        }
    }

    private static boolean isEmptyFilterValue(Object value) {
        return value == null
                || value instanceof String stringValue && stringValue.trim().isEmpty()
                || value instanceof Collection<?> collection && collection.isEmpty()
                || value instanceof Object[] array && array.length == 0;
    }

    private static String buildResultAlias(String tableName, String columnName) {
        return sanitizeAliasPart(tableName) + "_" + sanitizeAliasPart(columnName);
    }

    private static String sanitizeAliasPart(String value) {
        return value.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static String getRequiredColumnName(Class<? extends BaseEntity> entityClass, String propertyName) {
        String columnName = getEntityMetadata(entityClass).columnsByProperty().get(propertyName);
        if (columnName == null) {
            throw new IllegalArgumentException("Missing DbColumn annotation on "
                    + entityClass.getName() + "." + propertyName);
        }

        return columnName;
    }

    private static String getRequiredQualifiedColumnName(Class<? extends BaseEntity> entityClass,
                                                        String propertyName) {
        String columnName = getEntityMetadata(entityClass).qualifiedColumnsByProperty().get(propertyName);
        if (columnName == null) {
            throw new IllegalArgumentException("Missing DbColumn annotation on "
                    + entityClass.getName() + "." + propertyName);
        }

        return columnName;
    }

    private static String getRequiredQualifiedColumnName(Class<? extends BaseEntity> baseEntityClass,
                                                        BaseProperty property) {
        Class<? extends BaseEntity> selectedEntityClass = resolvePropertyEntityClass(baseEntityClass, property);
        return getRequiredQualifiedColumnName(selectedEntityClass, property.getPropertyName());
    }

    private static String getRequiredQualifiedColumnName(Class<? extends BaseEntity> baseEntityClass,
                                                        BaseProperty property, List<JoinSpec> joins) {
        Class<? extends BaseEntity> selectedEntityClass = resolvePropertyEntityClass(baseEntityClass, property);
        String tableAlias = resolvePropertySqlAlias(baseEntityClass, property, joins);
        return getRequiredQualifiedColumnName(selectedEntityClass, property.getPropertyName(), tableAlias);
    }

    private static String getRequiredQualifiedColumnName(Class<? extends BaseEntity> entityClass,
                                                        BaseProperty property, String tableAlias) {
        return getRequiredQualifiedColumnName(entityClass, property.getPropertyName(), tableAlias);
    }

    private static String getRequiredQualifiedColumnName(Class<? extends BaseEntity> entityClass,
                                                        String propertyName, String tableAlias) {
        String columnName = getEntityMetadata(entityClass).columnsByProperty().get(propertyName);
        if (columnName == null) {
            throw new IllegalArgumentException("Missing DbColumn annotation on "
                    + entityClass.getName() + "." + propertyName);
        }

        return tableAlias + "." + columnName;
    }

    private static Class<? extends BaseEntity> resolvePropertyEntityClass(Class<? extends BaseEntity> baseEntityClass,
                                                                         BaseProperty property) {
        return property.getEntityClass() == null ? baseEntityClass : property.getEntityClass();
    }

    private static String resolvePropertySqlAlias(Class<? extends BaseEntity> baseEntityClass, BaseProperty property,
                                                  List<JoinSpec> joins) {
        if (property.getTableAlias() != null && !property.getTableAlias().isBlank()) {
            return property.getTableAlias();
        }

        Class<? extends BaseEntity> selectedEntityClass = resolvePropertyEntityClass(baseEntityClass, property);
        if (selectedEntityClass == baseEntityClass) {
            return getEntityMetadata(baseEntityClass).tableName();
        }

        List<JoinSpec> matchingJoins = joins == null ? List.of() : joins.stream()
                .filter(join -> join.getEntityClass() == selectedEntityClass)
                .toList();
        if (matchingJoins.size() == 1) {
            return getJoinSqlAlias(matchingJoins.get(0));
        }

        return getEntityMetadata(selectedEntityClass).tableName();
    }

    private static String getJoinSqlAlias(JoinSpec join) {
        if (join.getTableAlias() != null && !join.getTableAlias().isBlank()) {
            return sanitizeAliasPart(join.getTableAlias());
        }

        return getEntityMetadata(join.getEntityClass()).tableName();
    }

    private static EntityMetadata getEntityMetadata(Class<? extends BaseEntity> entityClass) {
        return ENTITY_METADATA_CACHE.computeIfAbsent(entityClass, BaseDao::readEntityMetadata);
    }

    private static ColumnFieldMetadata getRequiredColumnFieldMetadata(Class<? extends BaseEntity> entityClass,
                                                                     String propertyName) {
        return getEntityMetadata(entityClass).columnFields().stream()
                .filter(columnField -> columnField.field().getName().equals(propertyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing DbColumn annotation on "
                        + entityClass.getName() + "." + propertyName));
    }

    private static EntityMetadata readEntityMetadata(Class<? extends BaseEntity> entityClass) {
        DbTable dbTable = entityClass.getAnnotation(DbTable.class);
        if (dbTable == null) {
            throw new IllegalArgumentException("Missing DbTable annotation on " + entityClass.getName());
        }

        List<ColumnFieldMetadata> columnFields = new ArrayList<>();
        List<ColumnFieldMetadata> insertableColumnFields = new ArrayList<>();
        List<ColumnFieldMetadata> updatableColumnFields = new ArrayList<>();
        Map<String, String> columnsByProperty = new LinkedHashMap<>();
        Map<String, String> qualifiedColumnsByProperty = new LinkedHashMap<>();
        for (Field field : getAllFields(entityClass)) {
            DbColumn dbColumn = field.getAnnotation(DbColumn.class);
            if (dbColumn == null) {
                continue;
            }

            ColumnFieldMetadata columnField = new ColumnFieldMetadata(field, dbTable.value(), dbColumn.value());
            columnFields.add(columnField);
            if (!"id".equals(field.getName())) {
                insertableColumnFields.add(columnField);
                updatableColumnFields.add(columnField);
            }
            columnsByProperty.put(field.getName(), dbColumn.value());
            qualifiedColumnsByProperty.put(field.getName(), columnField.qualifiedColumnName());
        }

        if (columnFields.isEmpty()) {
            throw new IllegalArgumentException("Missing DbColumn annotations on " + entityClass.getName());
        }

        List<String> selectExpressions = columnFields.stream()
                .map(ColumnFieldMetadata::selectExpression)
                .toList();

        return new EntityMetadata(dbTable.value(), List.copyOf(columnFields), selectExpressions,
                List.copyOf(insertableColumnFields), List.copyOf(updatableColumnFields),
                Map.copyOf(columnsByProperty), Map.copyOf(qualifiedColumnsByProperty));
    }

    private static List<Field> getAllFields(Class<?> entityClass) {
        return FIELD_CACHE.computeIfAbsent(entityClass, BaseDao::readAllFields);
    }

    private static List<Field> readAllFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = entityClass;
        while (currentClass != null && currentClass != Object.class) {
            List<Field> declaredFields = Arrays.asList(currentClass.getDeclaredFields());
            declaredFields.forEach(field -> field.setAccessible(true));
            fields.addAll(0, declaredFields);
            currentClass = currentClass.getSuperclass();
        }

        return List.copyOf(fields);
    }

    private static final class EntityMetadata {

        private final String tableName;
        private final List<ColumnFieldMetadata> columnFields;
        private final List<String> selectExpressions;
        private final List<ColumnFieldMetadata> insertableColumnFields;
        private final List<ColumnFieldMetadata> updatableColumnFields;
        private final Map<String, String> columnsByProperty;
        private final Map<String, String> qualifiedColumnsByProperty;

        private EntityMetadata(String tableName, List<ColumnFieldMetadata> columnFields, List<String> selectExpressions,
                               List<ColumnFieldMetadata> insertableColumnFields,
                               List<ColumnFieldMetadata> updatableColumnFields,
                               Map<String, String> columnsByProperty,
                               Map<String, String> qualifiedColumnsByProperty) {
            this.tableName = tableName;
            this.columnFields = columnFields;
            this.selectExpressions = selectExpressions;
            this.insertableColumnFields = insertableColumnFields;
            this.updatableColumnFields = updatableColumnFields;
            this.columnsByProperty = columnsByProperty;
            this.qualifiedColumnsByProperty = qualifiedColumnsByProperty;
        }

        private String tableName() {
            return tableName;
        }

        private List<ColumnFieldMetadata> columnFields() {
            return columnFields;
        }

        private List<String> selectExpressions() {
            return selectExpressions;
        }

        private List<ColumnFieldMetadata> insertableColumnFields() {
            return insertableColumnFields;
        }

        private List<ColumnFieldMetadata> updatableColumnFields() {
            return updatableColumnFields;
        }

        private Map<String, String> columnsByProperty() {
            return columnsByProperty;
        }

        private Map<String, String> qualifiedColumnsByProperty() {
            return qualifiedColumnsByProperty;
        }
    }

    private static final class SqlParameter {

        private final String name;
        private final Object value;

        private SqlParameter(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        private String name() {
            return name;
        }

        private Object value() {
            return value;
        }
    }

    private static final class ColumnFieldMetadata {

        private final Field field;
        private final String columnName;
        private final String qualifiedColumnName;
        private final String resultAlias;
        private final String selectExpression;

        private ColumnFieldMetadata(Field field, String tableName, String columnName) {
            this.field = field;
            this.columnName = columnName;
            this.qualifiedColumnName = tableName + "." + columnName;
            this.resultAlias = buildResultAlias(tableName, columnName);
            this.selectExpression = qualifiedColumnName + " AS " + resultAlias;
        }

        private Field field() {
            return field;
        }

        private String columnName() {
            return columnName;
        }

        private String qualifiedColumnName() {
            return qualifiedColumnName;
        }

        private String qualifiedColumnName(String tableAlias) {
            return tableAlias + "." + columnName;
        }

        private String resultAlias() {
            return resultAlias;
        }

        private String resultAlias(String tableAlias) {
            return buildResultAlias(tableAlias, columnName);
        }

        private String selectExpression() {
            return selectExpression;
        }

        private String selectExpression(String tableAlias) {
            return qualifiedColumnName(tableAlias) + " AS " + resultAlias(tableAlias);
        }
    }

}
