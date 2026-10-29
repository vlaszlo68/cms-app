package hu.laci.cms.backend.dao.menu;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuProperty;
import hu.laci.cms.backend.model.site.Site;

import java.util.List;
import java.util.Optional;

/** Persistence contract for Site-owned menus. */
public interface MenuDao extends CrudDao<Menu, MenuProperty> {

    /**
     * Counts menus directly owned by a Site.
     *
     * @param site owning persisted Site
     * @return number of menus owned by the Site
     */
    long countBySite(Site site);

    /**
     * Lists menus owned by one Site.
     *
     * @param site owning Site with a persisted id
     * @return Site-owned menus
     */
    List<Menu> findAllBySite(Site site);

    /**
     * Finds a menu id only within one Site.
     *
     * @param site owning Site with a persisted id
     * @param id menu id
     * @return matching Site-owned menu, if present
     */
    Optional<Menu> findBySiteAndId(Site site, Long id);

    /**
     * Finds a menu code only within one Site.
     *
     * @param site owning Site with a persisted id
     * @param code Site-scoped menu code
     * @return matching Site-owned menu, if present
     */
    Optional<Menu> findBySiteAndCode(Site site, String code);
}
