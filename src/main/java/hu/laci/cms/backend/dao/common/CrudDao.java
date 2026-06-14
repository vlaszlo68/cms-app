package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseProperty;
import hu.laci.cms.backend.model.common.QuerySpec;

import java.util.List;
import java.util.Optional;

/**
 * Common DAO contract for entity persistence.
 *
 * @param <T> entity type handled by the DAO
 * @param <P> property descriptor type accepted by {@link QuerySpec}
 */
public interface CrudDao<T extends BaseEntity, P extends BaseProperty> {

    /**
     * Finds all rows using the DAO default ordering.
     * <p>
     * This is equivalent to {@code findAll(null)}; {@link BaseDao} orders by
     * {@code id ASC} by default.
     *
     * @return all persisted entities
     */
    default List<T> findAll() {
        return findAll((QuerySpec<P>) null);
    }

    /**
     * Finds rows matching the given query specification.
     * <p>
     * Example:
     *
     * <pre>{@code
     * userDao.findAll(QuerySpec.<UserProperty>create()
     *         .where(UserProperty.LOGIN_NAME).like("adm")
     *         .orderBy(UserProperty.LOGIN_NAME.desc()));
     * }</pre>
     *
     * @param querySpec filters, joins, and sort orders; {@code null} means no filters and default ordering
     * @return matching entities
     */
    List<T> findAll(QuerySpec<P> querySpec);

    /**
     * Finds one entity by its primary key.
     *
     * @param id entity id
     * @return matching entity, or {@link Optional#empty()} when no row exists
     */
    Optional<T> findById(Long id);

    /**
     * Saves the entity by delegating to {@link #create(BaseEntity)} when the id
     * is {@code null}, otherwise to {@link #update(BaseEntity)}.
     *
     * @param entity entity to create or update
     * @return saved entity; create operations usually return the same instance with id filled
     */
    T save(T entity);

    /**
     * Inserts a new entity.
     *
     * @param entity entity with no id
     * @return created entity with generated id filled when supported by the implementation
     */
    T create(T entity);

    /**
     * Updates an existing entity.
     *
     * @param entity entity with non-null id
     * @return updated entity
     */
    T update(T entity);

    /**
     * Deletes an entity by primary key.
     *
     * @param id entity id
     * @return {@code true} when a row was deleted
     */
    boolean deleteById(Long id);

    /**
     * Deletes an entity by its primary key.
     *
     * @param entity entity with non-null id
     * @return {@code true} when a row was deleted
     */
    boolean delete(T entity);
}
