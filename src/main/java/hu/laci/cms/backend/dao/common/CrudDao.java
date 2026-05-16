package hu.laci.cms.backend.dao.common;

import java.util.List;
import java.util.Optional;

public interface CrudDao<T, F, S, ID> {

    default List<T> getList() {
        return getList(null, null, null);
    }

    List<T> getList(F filter, S sort, Boolean ascending);

    Optional<T> findById(ID id);
}
