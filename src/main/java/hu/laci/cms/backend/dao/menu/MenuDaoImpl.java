package hu.laci.cms.backend.dao.menu;

import hu.laci.cms.backend.dao.common.BaseDao;
import hu.laci.cms.backend.model.menu.Menu;
import hu.laci.cms.backend.model.menu.MenuProperty;
import hu.laci.cms.backend.model.common.QuerySpec;
import hu.laci.cms.backend.model.site.Site;

import java.util.List;
import java.util.Optional;

public class MenuDaoImpl extends BaseDao<Menu, MenuProperty> implements MenuDao {

    public MenuDaoImpl() {
        super(Menu.class);
    }

    @Override
    public List<Menu> findAllBySite(Site site) {
        return findAll(QuerySpec.<MenuProperty>create()
                .where(MenuProperty.SITE).equalsTo(site)
                .orderBy(MenuProperty.NAME.asc()));
    }

    @Override
    public Optional<Menu> findBySiteAndId(Site site, Long id) {
        return findAll(QuerySpec.<MenuProperty>create()
                .where(MenuProperty.SITE).equalsTo(site)
                .where(MenuProperty.ID).equalsTo(id))
                .stream().findFirst();
    }

    @Override
    public Optional<Menu> findBySiteAndCode(Site site, String code) {
        return findAll(QuerySpec.<MenuProperty>create()
                .where(MenuProperty.SITE).equalsTo(site)
                .where(MenuProperty.CODE).equalsTo(code))
                .stream().findFirst();
    }
}
