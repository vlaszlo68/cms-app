package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseFilter;
import hu.laci.cms.backend.model.common.BaseSort;
import hu.laci.cms.backend.model.common.SortOrder;

import java.util.List;
import java.util.Optional;

public interface CrudDao<T extends BaseEntity, F extends BaseFilter, S extends BaseSort> {

    default List<T> findAll() {
        return findAll(null, null);
    }

    List<T> findAll(F filter, List<SortOrder<S>> sort);

    Optional<T> findById(Long id);
}
