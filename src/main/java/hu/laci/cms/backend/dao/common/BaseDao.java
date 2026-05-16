package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.config.database.TransactionContext;
import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseFilter;
import hu.laci.cms.backend.model.common.BaseSort;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class BaseDao<T extends BaseEntity, F extends BaseFilter, S extends BaseSort>
        implements CrudDao<T, F, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDao.class);

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<? extends BaseEntity>, EntityMetadata> ENTITY_METADATA_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, FilterMetadata> FILTER_METADATA_CACHE = new ConcurrentHashMap<>();

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
                + getRequiredColumnName(entityClass, "id") + " = ?";
    }

    @Override
    public List<T> findAll(F filter, List<SortOrder<S>> sort) {
        return findAll(filter, sort, "Failed to get " + entityClass.getSimpleName() + " list.");
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
        EntityMetadata entityMetadata = getEntityMetadata(entityClass);

        return "SELECT " + String.join(", ", entityMetadata.columnNames())
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
                + "WHERE " + getRequiredColumnName(entityClass, "id") + " = ?";
    }

    protected static String buildFindBySql(String baseSelectSql, Class<? extends BaseEntity> entityClass,
                                           String propertyName) {
        return baseSelectSql + buildWhereEqualsCondition(entityClass, propertyName);
    }

    protected static String buildWhereEqualsCondition(Class<? extends BaseEntity> entityClass, String propertyName) {
        return "WHERE " + getRequiredColumnName(entityClass, propertyName) + " = ?";
    }

    protected void appendFilters(StringBuilder sqlBuilder, List<Object> parameters,
                                 Class<? extends BaseEntity> entityClass, BaseFilter filter) {
        if (filter == null) {
            return;
        }

        List<String> conditions = new ArrayList<>();
        for (FilterFieldMetadata filterField : getFilterMetadata(filter.getClass()).fields()) {
            Object value = getFieldValue(filterField.field(), filter);
            if (isEmptyFilterValue(value)) {
                continue;
            }

            String columnName = getRequiredColumnName(entityClass, filterField.entityProperty());
            conditions.add(buildFilterCondition(columnName, filterField));
            parameters.add(new SqlParameter(columnName,
                    buildFilterParameter(value, filterField.operation(), filterField.likePosition())));
        }

        if (conditions.isEmpty()) {
            return;
        }

        sqlBuilder.append(System.lineSeparator()).append("WHERE ");
        sqlBuilder.append(String.join(" AND ", conditions));
    }

    protected void appendOrder(StringBuilder sqlBuilder, Class<? extends BaseEntity> entityClass,
                               List<? extends SortOrder<? extends BaseSort>> sort) {
        List<? extends SortOrder<? extends BaseSort>> selectedSort =
                sort == null || sort.isEmpty() ? List.of(new SortOrder<>(BaseSort.ID)) : sort;

        sqlBuilder.append(System.lineSeparator())
                .append("ORDER BY ")
                .append(selectedSort.stream()
                        .map(sortOrder -> getRequiredColumnName(entityClass, sortOrder.getSort().getPropertyName())
                                + " " + sortOrder.getDirection())
                        .collect(Collectors.joining(", ")));
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

    protected List<T> findAll(F filter, List<SortOrder<S>> sort, String errorMessage) {
        StringBuilder sqlBuilder = new StringBuilder(baseSelectSql);
        List<Object> parameters = new ArrayList<>();

        appendFilters(sqlBuilder, parameters, entityClass, filter);
        appendOrder(sqlBuilder, entityClass, sort);

        return findList(sqlBuilder.toString(), parameters, getRowMapper(), errorMessage);
    }

    protected void setParameters(PreparedStatement preparedStatement, List<?> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            preparedStatement.setObject(index + 1, getParameterValue(parameters.get(index)));
        }
    }

    protected Optional<T> findOne(String sql, List<?> parameters, RowMapper<T> rowMapper, String errorMessage) {
        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement preparedStatement = connectionScope.getConnection().prepareStatement(sql)) {

            logSql("findOne", sql, parameters);
            setParameters(preparedStatement, parameters);

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
        try (TransactionContext.ConnectionScope connectionScope = TransactionContext.openConnection();
             PreparedStatement preparedStatement = connectionScope.getConnection().prepareStatement(sql)) {

            logSql("findList", sql, parameters);
            setParameters(preparedStatement, parameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<T> results = new ArrayList<>();
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

    protected T mapEntity(ResultSet resultSet) throws SQLException {
        T entity = createEntity();
        for (ColumnFieldMetadata columnField : getEntityMetadata(entityClass).columnFields()) {
            setFieldValue(columnField.field(), entity,
                    getColumnValue(resultSet, columnField.columnName(), columnField.field()));
        }

        return entity;
    }

    private T createEntity() throws SQLException {
        try {
            Constructor<T> constructor = entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Unable to create entity instance: " + entityClass.getName(), e);
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
        Long id = resultSet.getObject(columnName, Long.class);
        if (id == null) {
            return null;
        }

        try {
            BaseEntity entity = field.getType().asSubclass(BaseEntity.class).getDeclaredConstructor().newInstance();
            entity.setId(id);
            return entity;
        } catch (ReflectiveOperationException e) {
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

    private static String buildFilterCondition(String columnName, FilterFieldMetadata filterField) {
        return switch (filterField.operation()) {
            case EQUALS -> columnName + " = ?";
            case LIKE -> columnName + " LIKE ?";
        };
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

    private static Object getFieldValue(Field field, Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to read filter property "
                    + instance.getClass().getName() + "." + field.getName(), e);
        }
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
        return value == null || value instanceof String stringValue && stringValue.trim().isEmpty();
    }

    private static String getRequiredColumnName(Class<? extends BaseEntity> entityClass, String propertyName) {
        String columnName = getEntityMetadata(entityClass).columnsByProperty().get(propertyName);
        if (columnName == null) {
            throw new IllegalArgumentException("Missing DbColumn annotation on "
                    + entityClass.getName() + "." + propertyName);
        }

        return columnName;
    }

    private static EntityMetadata getEntityMetadata(Class<? extends BaseEntity> entityClass) {
        return ENTITY_METADATA_CACHE.computeIfAbsent(entityClass, BaseDao::readEntityMetadata);
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
        for (Field field : getAllFields(entityClass)) {
            DbColumn dbColumn = field.getAnnotation(DbColumn.class);
            if (dbColumn == null) {
                continue;
            }

            ColumnFieldMetadata columnField = new ColumnFieldMetadata(field, dbColumn.value());
            columnFields.add(columnField);
            if (!"id".equals(field.getName())) {
                insertableColumnFields.add(columnField);
                updatableColumnFields.add(columnField);
            }
            columnsByProperty.put(field.getName(), dbColumn.value());
        }

        if (columnFields.isEmpty()) {
            throw new IllegalArgumentException("Missing DbColumn annotations on " + entityClass.getName());
        }

        List<String> columnNames = columnFields.stream()
                .map(ColumnFieldMetadata::columnName)
                .toList();

        return new EntityMetadata(dbTable.value(), List.copyOf(columnFields), columnNames,
                List.copyOf(insertableColumnFields), List.copyOf(updatableColumnFields),
                Map.copyOf(columnsByProperty));
    }

    private static FilterMetadata getFilterMetadata(Class<?> filterClass) {
        return FILTER_METADATA_CACHE.computeIfAbsent(filterClass, BaseDao::readFilterMetadata);
    }

    private static FilterMetadata readFilterMetadata(Class<?> filterClass) {
        List<FilterFieldMetadata> fields = new ArrayList<>();
        for (Field field : getAllFields(filterClass)) {
            FilterProperty filterProperty = field.getAnnotation(FilterProperty.class);
            if (filterProperty == null) {
                continue;
            }

            fields.add(new FilterFieldMetadata(field, filterProperty.entityProperty(), filterProperty.operation(),
                    filterProperty.likePosition()));
        }

        return new FilterMetadata(List.copyOf(fields));
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
        private final List<String> columnNames;
        private final List<ColumnFieldMetadata> insertableColumnFields;
        private final List<ColumnFieldMetadata> updatableColumnFields;
        private final Map<String, String> columnsByProperty;

        private EntityMetadata(String tableName, List<ColumnFieldMetadata> columnFields, List<String> columnNames,
                               List<ColumnFieldMetadata> insertableColumnFields,
                               List<ColumnFieldMetadata> updatableColumnFields,
                               Map<String, String> columnsByProperty) {
            this.tableName = tableName;
            this.columnFields = columnFields;
            this.columnNames = columnNames;
            this.insertableColumnFields = insertableColumnFields;
            this.updatableColumnFields = updatableColumnFields;
            this.columnsByProperty = columnsByProperty;
        }

        private String tableName() {
            return tableName;
        }

        private List<ColumnFieldMetadata> columnFields() {
            return columnFields;
        }

        private List<String> columnNames() {
            return columnNames;
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

        private ColumnFieldMetadata(Field field, String columnName) {
            this.field = field;
            this.columnName = columnName;
        }

        private Field field() {
            return field;
        }

        private String columnName() {
            return columnName;
        }
    }

    private static final class FilterMetadata {

        private final List<FilterFieldMetadata> fields;

        private FilterMetadata(List<FilterFieldMetadata> fields) {
            this.fields = fields;
        }

        private List<FilterFieldMetadata> fields() {
            return fields;
        }
    }

    private static final class FilterFieldMetadata {

        private final Field field;
        private final String entityProperty;
        private final FilterOperation operation;
        private final LikeFilterPosition likePosition;

        private FilterFieldMetadata(Field field, String entityProperty, FilterOperation operation,
                                    LikeFilterPosition likePosition) {
            this.field = field;
            this.entityProperty = entityProperty;
            this.operation = operation;
            this.likePosition = likePosition;
        }

        private Field field() {
            return field;
        }

        private String entityProperty() {
            return entityProperty;
        }

        private FilterOperation operation() {
            return operation;
        }

        private LikeFilterPosition likePosition() {
            return likePosition;
        }
    }
}
