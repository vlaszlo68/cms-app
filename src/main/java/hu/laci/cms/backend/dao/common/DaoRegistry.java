package hu.laci.cms.backend.dao.common;

import hu.laci.cms.backend.dao.user.UserDaoImpl;
import hu.laci.cms.backend.dao.page.PageDaoImpl;
import hu.laci.cms.backend.dao.media.MediaDaoImpl;
import hu.laci.cms.backend.dao.menu.MenuDaoImpl;
import hu.laci.cms.backend.dao.menu.MenuItemDaoImpl;
import hu.laci.cms.backend.model.common.BaseEntity;
import hu.laci.cms.backend.model.common.BaseProperty;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.user.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Application-wide registry that resolves an entity class to its DAO instance.
 * <p>
 * The registry is initialized by {@code DaoRegistryListener} during servlet
 * application startup. Static DAO convenience methods, such as
 * {@link BaseDao#saveEntity(BaseEntity)}, use this registry.
 */
public final class DaoRegistry {

    private static final Map<Class<? extends BaseEntity>, CrudDao<? extends BaseEntity, ? extends BaseProperty>> DAOS = new HashMap<>();

    private DaoRegistry() {
    }

    /**
     * Initializes the registry with known DAO implementations.
     * <p>
     * This method is idempotent from the caller's perspective: it clears current
     * registrations first, then registers the current DAO set.
     */
    public static synchronized void initialize() {
        DAOS.clear();
        register(User.class, new UserDaoImpl());
        register(Page.class, new PageDaoImpl());
        register(Media.class, new MediaDaoImpl());
        register(Menu.class, new MenuDaoImpl());
        register(MenuItem.class, new MenuItemDaoImpl());
    }

    /**
     * Clears all DAO registrations.
     * <p>
     * Called from application shutdown to release references held by the static
     * registry.
     */
    public static synchronized void shutdown() {
        DAOS.clear();
    }

    /**
     * Returns the DAO registered for the given entity class.
     * <p>
     * Example:
     *
     * <pre>{@code
     * UserDao userDao = DaoRegistry.getDao(User.class);
     * }</pre>
     *
     * @param entityClass entity class used as registry key
     * @param <D> expected DAO interface or implementation type
     * @return registered DAO
     * @throws IllegalStateException when no DAO has been registered for the entity class
     */
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
