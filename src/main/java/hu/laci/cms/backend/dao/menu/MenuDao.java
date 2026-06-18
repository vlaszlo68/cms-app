package hu.laci.cms.backend.dao.menu;

import hu.laci.cms.backend.dao.common.CrudDao;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuProperty;

import java.util.Optional;

public interface MenuDao extends CrudDao<Menu, MenuProperty> {

    Optional<Menu> findByCode(String code);
}
