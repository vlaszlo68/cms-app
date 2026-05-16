package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.config.database.DatabaseConfig;
import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseFilter;
import hu.laci.cms.backend.model.common.BaseSort;
import hu.laci.cms.backend.model.common.SortOrder;

import java.lang.reflect.Field;
import java.sql.Connection;
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

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<? extends BaseEntity>, EntityMetadata> ENTITY_METADATA_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, FilterMetadata> FILTER_METADATA_CACHE = new ConcurrentHashMap<>();

    private final Class<T> entityClass;
    private final String baseSelectSql;
    private final String findByIdSql;

    protected BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.baseSelectSql = buildBaseSelectSql(entityClass);
        this.findByIdSql = buildFindBySql(baseSelectSql, entityClass, "id");
    }

    protected Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    @Override
    public List<T> findAll(F filter, List<SortOrder<S>> sort) {
        return findAll(filter, sort, "Failed to get " + entityClass.getSimpleName() + " list.");
    }

    @Override
    public Optional<T> findById(Long id) {
        return findById(id, "Failed to find " + entityClass.getSimpleName() + " by id: " + id);
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

            conditions.add(buildFilterCondition(entityClass, filterField));
            parameters.add(buildFilterParameter(value, filterField.operation(), filterField.likePosition()));
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
        return findOne(findByIdSql, List.of(id), getRowMapper(), errorMessage);
    }

    protected Optional<T> findOneByProperty(String propertyName, Object value, String errorMessage) {
        return findOne(buildFindBySql(baseSelectSql, entityClass, propertyName), List.of(value), getRowMapper(),
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
            preparedStatement.setObject(index + 1, parameters.get(index));
        }
    }

    protected Optional<T> findOne(String sql, List<?> parameters, RowMapper<T> rowMapper, String errorMessage) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            setParameters(preparedStatement, parameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(rowMapper.map(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    protected List<T> findList(String sql, List<?> parameters, RowMapper<T> rowMapper, String errorMessage) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            setParameters(preparedStatement, parameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(rowMapper.map(resultSet));
                }

                return results;
            }
        } catch (SQLException e) {
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
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Unable to create entity instance: " + entityClass.getName(), e);
        }
    }

    private static Object getColumnValue(ResultSet resultSet, String columnName, Field field) throws SQLException {
        if (BaseEntity.class.isAssignableFrom(field.getType())) {
            return getEntityReference(resultSet, columnName, field);
        }
        if (Date.class.isAssignableFrom(field.getType())) {
            return getDateValue(resultSet, columnName, field.getType());
        }

        try {
            return resultSet.getObject(columnName, getResultSetValueType(field.getType()));
        } catch (SQLFeatureNotSupportedException | AbstractMethodError e) {
            return resultSet.getObject(columnName);
        }
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

    private static String buildFilterCondition(Class<? extends BaseEntity> entityClass,
                                               FilterFieldMetadata filterField) {
        String columnName = getRequiredColumnName(entityClass, filterField.entityProperty());
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
        Map<String, String> columnsByProperty = new LinkedHashMap<>();
        for (Field field : getAllFields(entityClass)) {
            DbColumn dbColumn = field.getAnnotation(DbColumn.class);
            if (dbColumn == null) {
                continue;
            }

            columnFields.add(new ColumnFieldMetadata(field, dbColumn.value()));
            columnsByProperty.put(field.getName(), dbColumn.value());
        }

        if (columnFields.isEmpty()) {
            throw new IllegalArgumentException("Missing DbColumn annotations on " + entityClass.getName());
        }

        List<String> columnNames = columnFields.stream()
                .map(ColumnFieldMetadata::columnName)
                .toList();

        return new EntityMetadata(dbTable.value(), List.copyOf(columnFields), columnNames,
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
        private final Map<String, String> columnsByProperty;

        private EntityMetadata(String tableName, List<ColumnFieldMetadata> columnFields, List<String> columnNames,
                               Map<String, String> columnsByProperty) {
            this.tableName = tableName;
            this.columnFields = columnFields;
            this.columnNames = columnNames;
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

        private Map<String, String> columnsByProperty() {
            return columnsByProperty;
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
