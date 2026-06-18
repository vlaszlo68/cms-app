package hu.laci.cms.backend.dao.menu;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.menu.MenuItem;
import hu.laci.cms.backend.model.menu.MenuItemProperty;

import java.util.List;

public class MenuItemDaoImpl extends BaseDao<MenuItem, MenuItemProperty> implements MenuItemDao {

    public MenuItemDaoImpl() {
        super(MenuItem.class);
    }

    @Override
    public List<MenuItem> findByMenuId(Long menuId) {
        return findAll(QuerySpec.<MenuItemProperty>create()
                .where(MenuItemProperty.MENU_ID).equalsTo(menuId)
                .orderBy(MenuItemProperty.SORT_ORDER.asc())
                .orderBy(MenuItemProperty.ID.asc()));
    }

    @Override
    public List<MenuItem> findRootItems(Long menuId) {
        return findByMenuId(menuId).stream()
                .filter(item -> item.getParentId() == null)
                .toList();
    }
}
