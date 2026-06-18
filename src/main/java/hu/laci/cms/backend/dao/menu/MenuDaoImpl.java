package hu.laci.cms.backend.dao.menu;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuProperty;

import java.util.Optional;

public class MenuDaoImpl extends BaseDao<Menu, MenuProperty> implements MenuDao {

    public MenuDaoImpl() {
        super(Menu.class);
    }

    @Override
    public Optional<Menu> findByCode(String code) {
        return findOneByProperty("code", code, "Failed to find menu by code: " + code);
    }
}
