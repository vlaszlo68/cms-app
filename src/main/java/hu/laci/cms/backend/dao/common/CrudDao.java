package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseProperty;
import hu.laci.cms.backend.model.common.QuerySpec;

import java.util.List;
import java.util.Optional;

public interface CrudDao<T extends BaseEntity, P extends BaseProperty> {

    default List<T> findAll() {
        return findAll((QuerySpec<P>) null);
    }

    List<T> findAll(QuerySpec<P> querySpec);

    Optional<T> findById(Long id);

    T save(T entity);

    T create(T entity);

    T update(T entity);

    boolean deleteById(Long id);
}
