package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.dao.user.UserDaoImpl;
import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseProperty;
import hu.laci.cms.backend.model.user.User;

import java.util.HashMap;
import java.util.Map;

public final class DaoRegistry {

    private static final Map<Class<? extends BaseEntity>, CrudDao<? extends BaseEntity, ? extends BaseProperty>> DAOS = new HashMap<>();

    private DaoRegistry() {
    }

    public static synchronized void initialize() {
        DAOS.clear();
        register(User.class, new UserDaoImpl());
    }

    public static synchronized void shutdown() {
        DAOS.clear();
    }

    @SuppressWarnings("unchecked")
    public static <D extends CrudDao<? extends BaseEntity, ? extends BaseProperty>> D getDao(Class<? extends BaseEntity> entityClass) {
        CrudDao<? extends BaseEntity, ? extends BaseProperty> dao = DAOS.get(entityClass);
        if (dao == null) {
            throw new IllegalStateException("No DAO registered for " + entityClass.getName());
        }

        return (D) dao;
    }

    private static void register(Class<? extends BaseEntity> entityClass, CrudDao<? extends BaseEntity, ? extends BaseProperty> dao) {
        DAOS.put(entityClass, dao);
    }
}
