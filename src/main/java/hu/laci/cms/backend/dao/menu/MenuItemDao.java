package hu.laci.cms.backend.dao.menu;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.menu.MenuItemProperty;

import java.util.List;

public interface MenuItemDao extends CrudDao<MenuItem, MenuItemProperty> {

    List<MenuItem> findByMenuId(Long menuId);

    List<MenuItem> findRootItems(Long menuId);
}
